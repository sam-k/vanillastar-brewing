package com.vanillastar.vsbrewing.item

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
import net.minecraft.potion.Potion
import net.minecraft.potion.Potions
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Util

fun getPotionFlaskStackProvider(
    potion: RegistryEntry.Reference<Potion>,
    onRegisterStack: (stack: ItemStack) -> Unit = {},
) = { stack: ItemStack ->
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
  stack.set(
      DataComponentTypes.POTION_CONTENTS,
      PotionContentsComponent(Optional.of(potion), customColorOptional, listOf()),
  )
  onRegisterStack(stack)
}

fun appendPotionFlaskDataTooltip(
    stack: ItemStack,
    context: Item.TooltipContext,
    tooltip: MutableList<Text>,
) {
  val potionContents = stack.get(DataComponentTypes.POTION_CONTENTS)
  val effects = potionContents?.effects
  val showEffectsTooltip = effects != null && !potionContentsMatchId(potionContents, MILK_POTION_ID)

  if (showEffectsTooltip) {
    appendEffectsTooltip(effects, context.updateTickRate) { tooltip.add(it) }
  }
  appendRemainingUsesTooltip(stack, tooltip)
  if (showEffectsTooltip) {
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
