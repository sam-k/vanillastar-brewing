package com.vanillastar.vsbrewing.particle

import net.fabricmc.fabric.api.client.particle.v1.FabricSpriteProvider
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry.PendingParticleFactory
import net.minecraft.client.particle.ParticleFactory
import net.minecraft.client.particle.SpriteProvider
import net.minecraft.particle.ParticleEffect

abstract class ModParticleFactory<TParticleEffect : ParticleEffect> :
    ParticleFactory<TParticleEffect>, PendingParticleFactory<TParticleEffect> {
  internal var spriteProvider: SpriteProvider? = null

  override fun create(spriteProvider: FabricSpriteProvider): ParticleFactory<TParticleEffect> {
    this.spriteProvider = spriteProvider
    return this
  }
}
