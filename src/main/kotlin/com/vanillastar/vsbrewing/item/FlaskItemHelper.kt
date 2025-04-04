package com.vanillastar.vsbrewing.item

import com.vanillastar.vsbrewing.block.FlaskBlock
import com.vanillastar.vsbrewing.block.MOD_BLOCKS
import com.vanillastar.vsbrewing.block.entity.MOD_BLOCK_ENTITIES
import com.vanillastar.vsbrewing.component.MOD_COMPONENTS
import com.vanillastar.vsbrewing.potion.MILK_POTION_ID
import com.vanillastar.vsbrewing.potion.potionContentsMatchId
import com.vanillastar.vsbrewing.utils.getModIdentifier
import java.util.Optional
import kotlin.jvm.optionals.getOrNull
import net.minecraft.SharedConstants
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.AttributeModifiersComponent
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffectUtil
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsage
import net.minecraft.item.ItemUsageContext
import net.minecraft.item.Items
import net.minecraft.potion.Potion
import net.minecraft.potion.Potions
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.screen.ScreenTexts
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Util
import net.minecraft.world.event.GameEvent

/** Minimum number of uses for a flask item. */
const val FLASK_MIN_USES = 1

/** Maximum number of uses for a flask item. */
const val FLASK_MAX_USES = 3

fun getPotionFlaskStackProvider(
    potion: RegistryEntry.Reference<Potion>,
    remainingUses: Int = FLASK_MAX_USES,
): (ItemStack) -> Unit = {
  val customColorOptional =
      if (
          potion.matchesKey(Potions.MUNDANE.key.getOrNull()) ||
              potion.matchesKey(Potions.THICK.key.getOrNull())
      ) {
        // Override color to be default.
        Optional.of(PotionContentsComponent.getColor(listOf()))
      } else {
        Optional.empty()
      }
  it.set(
      DataComponentTypes.POTION_CONTENTS,
      PotionContentsComponent(Optional.of(potion), customColorOptional, listOf()),
  )
  it.set(MOD_COMPONENTS.flaskRemainingUsesComponent, remainingUses)
}

fun fillFromFlaskBlock(
    context: ItemUsageContext,
    isFlaskItem: Boolean,
    defaultAction: () -> ActionResult,
): ActionResult {
  if (context.shouldCancelInteraction()) {
    return defaultAction()
  }

  val world = context.world
  val blockPos = context.blockPos
  val state = world.getBlockState(blockPos)
  if (!state.isOf(MOD_BLOCKS.flaskBlock)) {
    return defaultAction()
  }

  val level = state.getOrEmpty(FlaskBlock.LEVEL).orElse(0)
  if (level < 1) {
    // Flask block is already empty.
    return ActionResult.PASS
  }

  val blockEntity =
      world.getBlockEntity(blockPos, MOD_BLOCK_ENTITIES.flaskBlockEntityType).orElse(null)
  if (blockEntity == null) {
    // Should never reach here.
    return ActionResult.PASS
  }

  val blockEntityStack = blockEntity.stack
  if (!blockEntityStack.isOf(MOD_ITEMS.potionFlaskItem)) {
    // Flask block does not contain an item.
    return ActionResult.PASS
  }

  val potionContents = blockEntityStack.get(DataComponentTypes.POTION_CONTENTS)
  if (potionContents == null) {
    // Flask block does not contain a potion.
    return ActionResult.PASS
  }

  if (world.isClient) {
    return ActionResult.success(/* swingHand= */ true)
  }

  val player = context.player
  val newStack: ItemStack
  var newLevel = level
  if (isFlaskItem) {
    newStack = MOD_ITEMS.potionFlaskItem.defaultStack
    newStack.set(MOD_COMPONENTS.flaskRemainingUsesComponent, level)
    newLevel = FlaskBlock.MIN_LEVEL
  } else {
    newStack = Items.POTION.defaultStack
    newLevel--
  }
  newStack.set(DataComponentTypes.POTION_CONTENTS, potionContents)
  player?.setStackInHand(context.hand, ItemUsage.exchangeStack(context.stack, player, newStack))

  val newState = state.with(FlaskBlock.LEVEL, newLevel)
  world.setBlockState(blockPos, newState)
  if (newLevel > FlaskBlock.MIN_LEVEL) {
    blockEntity.stack.set(MOD_COMPONENTS.flaskRemainingUsesComponent, newLevel)
  } else {
    blockEntity.setDefaultStack()
  }
  blockEntity.markDirty()
  world.updateListeners(blockPos, state, newState, /* flags= */ 0)
  world.emitGameEvent(GameEvent.BLOCK_CHANGE, blockPos, GameEvent.Emitter.of(newState))
  world.playSound(
      /* source= */ null,
      blockPos,
      SoundEvents.ITEM_BOTTLE_FILL,
      SoundCategory.BLOCKS,
      /* volume= */ 1.0f,
      /* pitch= */ 1.0f,
  )
  world.emitGameEvent(/* entity= */ null, GameEvent.FLUID_PICKUP, blockPos)

  return ActionResult.success(/* swingHand= */ false)
}

fun appendPotionFlaskDataTooltip(
    stack: ItemStack,
    context: Item.TooltipContext,
    tooltip: MutableList<Text>,
    forceShowRemainingUses: Boolean = false,
) {
  val potionContents = stack.get(DataComponentTypes.POTION_CONTENTS)
  val effects = potionContents?.effects
  val showEffects = effects != null && !potionContentsMatchId(potionContents, MILK_POTION_ID)
  val showRemainingUses = forceShowRemainingUses || stack.isOf(MOD_ITEMS.potionFlaskItem)

  if (showEffects) {
    appendEffectsTooltip(effects, context.updateTickRate) { tooltip.add(it) }
  }
  if (showRemainingUses) {
    appendRemainingUsesTooltip(stack, tooltip)
  }
  if (showEffects) {
    appendUsageTooltip(effects) { tooltip.add(it) }
  }
}

/**
 * Builds and appends the item tooltip displaying this potion flask's effects.
 *
 * This logic is mostly copied from [PotionContentsComponent.buildTooltip], since we wish to insert
 * our own tooltip within the default potion tooltip. Injecting into the function with a mixin would
 * be too fragile in this case.
 */
private fun appendEffectsTooltip(
    effects: Iterable<StatusEffectInstance>,
    tickRate: Float,
    textConsumer: (Text) -> Unit,
) {
  if (effects.none()) {
    textConsumer(Text.translatable("effect.none").formatted(Formatting.GRAY))
    return
  }

  for (effect in effects) {
    var mutableText = Text.translatable(effect.translationKey)
    if (effect.amplifier > 0) {
      mutableText =
          Text.translatable(
              "potion.withAmplifier",
              mutableText,
              Text.translatable("potion.potency." + effect.amplifier),
          )
    }
    if (!effect.isDurationBelow(SharedConstants.TICKS_PER_SECOND)) {
      mutableText =
          Text.translatable(
              "potion.withDuration",
              mutableText,
              StatusEffectUtil.getDurationText(effect, /* multiplier= */ 1.0f, tickRate),
          )
    }
    textConsumer(mutableText.formatted(effect.effectType.value().category.formatting))
  }
}

/** Builds and appends the item tooltip displaying the remaining uses of this potion flask. */
private fun appendRemainingUsesTooltip(stack: ItemStack, tooltip: MutableList<Text>) {
  val translationKeyPrefix =
      Util.createTranslationKey("item", getModIdentifier(GLASS_FLASK_ITEM_METADATA.name))
  tooltip.add(
      Text.translatable(
              "${translationKeyPrefix}.remaining_uses",
              stack.get(MOD_COMPONENTS.flaskRemainingUsesComponent),
          )
          .formatted(Formatting.GRAY)
  )
}

/**
 * Builds and appends the item tooltip displaying what happens when this potion flask is used.
 *
 * This logic is mostly copied from [PotionContentsComponent.buildTooltip], since we wish to insert
 * our own tooltip within the default potion tooltip. Injecting into the function with a mixin would
 * be too fragile in this case.
 */
private fun appendUsageTooltip(
    effects: Iterable<StatusEffectInstance>,
    textConsumer: (Text) -> Unit,
) {
  val effectDataList =
      mutableListOf<Pair<RegistryEntry<EntityAttribute>, EntityAttributeModifier>>()
  for (statusEffectInstance in effects) {
    statusEffectInstance.effectType.value().forEachAttributeModifier(
        statusEffectInstance.amplifier
    ) { attribute, modifier ->
      effectDataList.add(Pair(attribute, modifier))
    }
  }

  if (effectDataList.none { (_, modifier) -> modifier.value != 0.0 }) {
    return
  }

  textConsumer(ScreenTexts.EMPTY)
  textConsumer(Text.translatable("potion.whenDrank").formatted(Formatting.DARK_PURPLE))

  for ((attribute, modifier) in effectDataList) {
    var modifierDisplayValue =
        modifier.value *
            when (modifier.operation) {
              EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE -> 100
              EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL -> 100
              else -> 1
            }

    val modifierTranslationKeyPrefix: String
    val formatting: Formatting
    if (modifier.value >= 0.0) {
      modifierTranslationKeyPrefix = "attribute.modifier.plus"
      formatting = Formatting.BLUE
    } else {
      modifierDisplayValue *= -1
      modifierTranslationKeyPrefix = "attribute.modifier.take"
      formatting = Formatting.RED
    }

    textConsumer(
        Text.translatable(
                "${modifierTranslationKeyPrefix}.${modifier.operation.id}",
                AttributeModifiersComponent.DECIMAL_FORMAT.format(modifierDisplayValue),
                Text.translatable(attribute.value().translationKey),
            )
            .formatted(formatting)
    )
  }
}
