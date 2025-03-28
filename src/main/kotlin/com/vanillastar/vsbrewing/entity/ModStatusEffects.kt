package com.vanillastar.vsbrewing.entity

import com.vanillastar.vsbrewing.utils.ModRegistry
import com.vanillastar.vsbrewing.utils.getModIdentifier
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.entry.RegistryEntry

abstract class ModStatusEffects : ModRegistry() {
  @JvmField
  val milkStatusEffect: RegistryEntry<StatusEffect> =
      Registry.registerReference(
          Registries.STATUS_EFFECT,
          getModIdentifier("milk"),
          MilkStatusEffect(),
      )

  override fun initialize() {}
}

@JvmField val MOD_STATUS_EFFECTS = object : ModStatusEffects() {}
