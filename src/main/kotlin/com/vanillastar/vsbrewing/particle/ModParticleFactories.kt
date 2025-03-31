package com.vanillastar.vsbrewing.particle

import com.vanillastar.vsbrewing.utils.ModRegistry
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry
import net.minecraft.particle.ParticleEffect
import net.minecraft.particle.ParticleType
import net.minecraft.registry.Registries

@Environment(EnvType.CLIENT)
abstract class ModParticleFactories() : ModRegistry() {
  override fun initialize() {
    this.registerParticleFactory(
        MOD_PARTICLE_TYPES.flaskEffectParticleType,
        FlaskEffectParticle.Companion.DefaultFactory(),
    )
    this.registerParticleFactory(
        MOD_PARTICLE_TYPES.flaskInstantEffectParticleType,
        FlaskEffectParticle.Companion.InstantFactory(),
    )
  }

  private fun <TParticleEffect : ParticleEffect> registerParticleFactory(
      particleType: ParticleType<TParticleEffect>,
      factory: ParticleFactoryRegistry.PendingParticleFactory<TParticleEffect>,
  ) {
    ParticleFactoryRegistry.getInstance().register(particleType, factory)
    this.logger.info(
        "Registered particle factory for type {}",
        Registries.PARTICLE_TYPE.getEntry(particleType).idAsString,
    )
  }
}

@JvmField val MOD_PARTICLE_FACTORIES = object : ModParticleFactories() {}
