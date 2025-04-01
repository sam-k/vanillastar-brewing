package com.vanillastar.vsbrewing.particle

import com.vanillastar.vsbrewing.utils.ModRegistry
import com.vanillastar.vsbrewing.utils.getModIdentifier
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes
import net.minecraft.particle.SimpleParticleType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry

abstract class ModParticleTypes() : ModRegistry() {
  @JvmField val flaskEffectParticleType = this.registerSimpleParticleType("flask_effect")

  @JvmField
  val flaskInstantEffectParticleType = this.registerSimpleParticleType("flask_instant_effect")

  private fun registerSimpleParticleType(name: String): SimpleParticleType {
    val id = getModIdentifier(name)
    val particleType = Registry.register(Registries.PARTICLE_TYPE, id, FabricParticleTypes.simple())
    this.logger.info("Registered particle type {}", id)
    return particleType
  }

  override fun initialize() {}
}

@JvmField val MOD_PARTICLE_TYPES = object : ModParticleTypes() {}
