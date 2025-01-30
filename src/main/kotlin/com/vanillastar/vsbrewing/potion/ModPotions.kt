package com.vanillastar.vsbrewing.potion

import com.vanillastar.vsbrewing.utils.ModRegistry
import com.vanillastar.vsbrewing.utils.getModIdentifier
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.potion.Potion
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.entry.RegistryEntry

abstract class ModPotions : ModRegistry() {
  @JvmField
  val strongWeakness =
      registerPotion(
          "strong_weakness",
          StatusEffectInstance(
              StatusEffects.WEAKNESS,
              /* duration= */ 900, // Half the duration of `Potions.WEAKNESS`
              /* amplifier= */ 1,
          ),
      )

  @Suppress("SameParameterValue")
  private fun registerPotion(
      name: String,
      statusEffectInstance: StatusEffectInstance,
  ): RegistryEntry.Reference<Potion> {
    val potion =
        Registry.registerReference(
            Registries.POTION,
            getModIdentifier(name),
            Potion(statusEffectInstance),
        )
    logger.info("Registered potion {}", name)
    return potion
  }

  override fun initialize() {}
}

@JvmField val MOD_POTIONS = object : ModPotions() {}
