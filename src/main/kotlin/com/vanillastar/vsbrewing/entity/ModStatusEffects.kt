package com.vanillastar.vsbrewing.entity

import com.vanillastar.vsbrewing.utils.ModRegistry
import com.vanillastar.vsbrewing.utils.getModIdentifier
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.entry.RegistryEntry

abstract class ModStatusEffects : ModRegistry() {
  @JvmField
  val healthDownStatusEffect = registerStatusEffect("health_down", HealthDownStatusEffect())

  @JvmField val milkStatusEffect = registerStatusEffect("milk", MilkStatusEffect())

  private fun registerStatusEffect(
      name: String,
      statusEffect: StatusEffect,
  ): RegistryEntry<StatusEffect> {
    val id = getModIdentifier(name)
    val statusEffectEntry = Registry.registerReference(Registries.STATUS_EFFECT, id, statusEffect)
    this.logger.info("Registered status effect {}", id)
    return statusEffectEntry
  }

  override fun initialize() {}
}

@JvmField val MOD_STATUS_EFFECTS = object : ModStatusEffects() {}
