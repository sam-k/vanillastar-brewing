package com.vanillastar.vsbrewing.item

import com.vanillastar.vsbrewing.block.MOD_BLOCKS
import com.vanillastar.vsbrewing.component.MOD_COMPONENTS
import com.vanillastar.vsbrewing.utils.getModIdentifier
import java.util.Optional
import kotlin.Float
import kotlin.Pair
import kotlin.String
import kotlin.Unit
import kotlin.jvm.optionals.getOrNull
import kotlin.repeat
import kotlin.streams.asStream
import net.minecraft.SharedConstants
import net.minecraft.advancement.criterion.Criteria
import net.minecraft.block.Blocks
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.AttributeModifiersComponent
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffectUtil
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.*
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.particle.ParticleTypes
import net.minecraft.potion.Potion
import net.minecraft.potion.Potions
import net.minecraft.registry.Registries
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.registry.tag.BlockTags
import net.minecraft.screen.ScreenTexts
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.stat.Stats
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.util.UseAction
import net.minecraft.util.Util
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import net.minecraft.world.event.GameEvent

val POTION_FLASK_ITEM_METADATA =
    ModItemMetadata(
        "potion_flask",
        ItemGroups.FOOD_AND_DRINK,
        /* previousItem= */ null,
        Registries.POTION.streamEntries()
            .flatMap { potion ->
              (PotionFlaskItem.MAX_USES downTo PotionFlaskItem.MIN_USES)
                  .asSequence()
                  .map { remainingUses ->
                    ModItemGroupVisibilityMetadata(
                        if (remainingUses < PotionFlaskItem.MAX_USES)
                            ItemGroup.StackVisibility.SEARCH_TAB_ONLY
                        else ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS
                    ) { stack ->
                      val customColorOptional =
                          if (
                              potion.matchesKey(Potions.MUNDANE.key.getOrNull()) ||
                                  potion.matchesKey(Potions.THICK.key.getOrNull())
                          ) {
                            Optional.of(PotionContentsComponent.getColor(listOf()))
                          } else {
                            Optional.empty()
                          }
                      stack.set(
                          DataComponentTypes.POTION_CONTENTS,
                          PotionContentsComponent(
                              Optional.of(potion),
                              customColorOptional,
                              listOf(),
                          ),
                      )
                      stack.set(MOD_COMPONENTS.potionFlaskRemainingUsesComponent, remainingUses)
                    }
                  }
                  .asStream()
            }
            .toList(),
    ) {
      it.maxCount(1)
          .component(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT)
          .component(MOD_COMPONENTS.potionFlaskRemainingUsesComponent, PotionFlaskItem.MAX_USES)
    }

class PotionFlaskItem(settings: Settings) : AliasedBlockItem(MOD_BLOCKS.flaskBlock, settings) {
  companion object {
    /** Minimum number of uses for a [PotionFlaskItem]. */
    const val MIN_USES = 1

    /** Maximum number of uses for a [PotionFlaskItem]. */
    const val MAX_USES = 3

    /** Duration for drinking a [PotionFlaskItem], in ticks. */
    private const val MAX_USE_TIME = 32

    fun appendPotionFlaskDataTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltip: MutableList<Text>,
    ) {
      val effects = stack.get(DataComponentTypes.POTION_CONTENTS)?.effects
      if (effects != null) {
        appendEffectsTooltip(effects, context.updateTickRate) { tooltip.add(it) }
      }
      appendRemainingUsesTooltip(stack, tooltip)
      if (effects != null) {
        appendUsageTooltip(effects) { tooltip.add(it) }
      }
    }

    /**
     * Builds and appends the item tooltip displaying this potion flask's effects.
     *
     * This logic is mostly copied from [PotionContentsComponent.buildTooltip], since we wish to
     * insert our own tooltip within the default potion tooltip. Injecting into the function with a
     * mixin would be too fragile in this case.
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
      val potionFlaskItemTranslationKey =
          Util.createTranslationKey("item", getModIdentifier(POTION_FLASK_ITEM_METADATA.name))
      tooltip.add(
          Text.translatable(
                  "${potionFlaskItemTranslationKey}.remaining_uses",
                  stack.get(MOD_COMPONENTS.potionFlaskRemainingUsesComponent),
              )
              .formatted(Formatting.GRAY)
      )
    }

    /**
     * Builds and appends the item tooltip displaying what happens when this potion flask is used.
     *
     * This logic is mostly copied from [PotionContentsComponent.buildTooltip], since we wish to
     * insert our own tooltip within the default potion tooltip. Injecting into the function with a
     * mixin would be too fragile in this case.
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

        var modifierTranslationKeyPrefix: String
        var formatting: Formatting
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
  }

  override fun getDefaultStack(): ItemStack {
    val stack = super.getDefaultStack()
    stack.set(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent(Potions.WATER))
    stack.set(MOD_COMPONENTS.potionFlaskRemainingUsesComponent, MAX_USES)
    return stack
  }

  /** This is copied from [PotionItem.getMaxUseTime]. */
  override fun getMaxUseTime(stack: ItemStack, user: LivingEntity) = MAX_USE_TIME

  /** This is copied from [PotionItem.getUseAction]. */
  override fun getUseAction(stack: ItemStack) = UseAction.DRINK

  /** This is copied from [PotionItem.use]. */
  override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> =
      ItemUsage.consumeHeldItem(world, user, hand)

  /**
   * This is mostly copied from [PotionItem.finishUsing], except we try to decrement the potion
   * flask's remaining uses instead of immediately emptying the flask.
   */
  override fun finishUsing(stack: ItemStack, world: World, user: LivingEntity): ItemStack {
    val player = user as? PlayerEntity
    if (player is ServerPlayerEntity) {
      Criteria.CONSUME_ITEM.trigger(player, stack)
    }

    if (!world.isClient) {
      stack
          .getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT)
          .forEachEffect {
            if (it.effectType.value().isInstant) {
              it.effectType
                  .value()
                  .applyInstantEffect(player, player, user, it.amplifier, /* proximity= */ 1.0)
            } else {
              user.addStatusEffect(it)
            }
          }
    }

    player?.incrementStat(Stats.USED.getOrCreateStat(this))

    if (player?.isInCreativeMode != true) {
      // If applicable, decrement remaining uses instead of discarding item.
      val remainingUses =
          stack.getOrDefault(MOD_COMPONENTS.potionFlaskRemainingUsesComponent, MAX_USES)
      if (remainingUses > MIN_USES) {
        stack.set(MOD_COMPONENTS.potionFlaskRemainingUsesComponent, remainingUses - 1)
      } else {
        stack.decrementUnlessCreative(/* amount= */ 1, player)
      }
      if (stack.isEmpty) {
        return ItemStack(MOD_ITEMS.glassFlaskItem)
      }
    }

    user.emitGameEvent(GameEvent.DRINK)
    return stack
  }

  /**
   * This is copied mostly from [PotionItem.useOnBlock], except we try to decrement the potion
   * flask's remaining uses instead of immediately emptying the flask.
   */
  override fun useOnBlock(context: ItemUsageContext): ActionResult {
    val world = context.world
    val blockPos = context.blockPos
    val player = context.player
    val stack = context.stack

    if (
        context.side == Direction.DOWN ||
            !world.getBlockState(blockPos).isIn(BlockTags.CONVERTABLE_TO_MUD) ||
            !stack
                .getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT)
                .matches(Potions.WATER)
    ) {
      return if (player != null && player.isSneaking) {
        this.place(ItemPlacementContext(context))
      } else {
        ActionResult.PASS
      }
    }

    world.playSound(
        /* source= */ null,
        blockPos,
        SoundEvents.ENTITY_GENERIC_SPLASH,
        SoundCategory.BLOCKS,
        /* volume= */ 1.0f,
        /* pitch= */ 1.0f,
    )

    player?.incrementStat(Stats.USED.getOrCreateStat(stack.item))

    // If applicable, decrement remaining uses instead of discarding item.
    val remainingUses = stack.getOrDefault(MOD_COMPONENTS.potionFlaskRemainingUsesComponent, 0)
    if (remainingUses > MIN_USES && player?.isInCreativeMode != true) {
      stack.set(MOD_COMPONENTS.potionFlaskRemainingUsesComponent, remainingUses - 1)
    } else {
      player?.setStackInHand(
          context.hand,
          ItemUsage.exchangeStack(stack, player, ItemStack(MOD_ITEMS.glassFlaskItem)),
      )
    }

    if (!world.isClient) {
      val serverWorld = world as ServerWorld
      repeat(5) {
        serverWorld.spawnParticles(
            ParticleTypes.SPLASH,
            blockPos.x + world.random.nextDouble(),
            blockPos.y + 1.0,
            blockPos.z + world.random.nextDouble(),
            /* count= */ 1,
            /* deltaX= */ 0.0,
            /* deltaY= */ 0.0,
            /* deltaZ= */ 0.0,
            /* speed= */ 1.0,
        )
      }
    }

    world.playSound(
        /* source= */ null,
        blockPos,
        SoundEvents.ITEM_BOTTLE_EMPTY,
        SoundCategory.BLOCKS,
        /* volume= */ 1.0f,
        /* pitch= */ 1.0f,
    )
    world.emitGameEvent(/* entity= */ null, GameEvent.FLUID_PLACE, blockPos)
    world.setBlockState(blockPos, Blocks.MUD.defaultState)
    return ActionResult.success(world.isClient)
  }

  /** This is copied from [PotionItem.getTranslationKey]. */
  override fun getTranslationKey(stack: ItemStack): String =
      Potion.finishTranslationKey(
          stack
              .getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT)
              .potion(),
          this.translationKey + ".effect.",
      )

  override fun appendTooltip(
      stack: ItemStack,
      context: TooltipContext,
      tooltip: MutableList<Text>,
      type: TooltipType,
  ) {
    appendPotionFlaskDataTooltip(stack, context, tooltip)
  }
}
