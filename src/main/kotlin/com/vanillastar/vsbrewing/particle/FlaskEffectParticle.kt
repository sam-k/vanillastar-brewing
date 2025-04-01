package com.vanillastar.vsbrewing.particle

import com.vanillastar.vsbrewing.block.entity.FLASK_SPLASH_MULTIPLIER
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.particle.SpellParticle
import net.minecraft.client.particle.SpriteProvider
import net.minecraft.client.world.ClientWorld
import net.minecraft.particle.SimpleParticleType

@Environment(EnvType.CLIENT)
class FlaskEffectParticle(
    world: ClientWorld,
    x: Double,
    y: Double,
    z: Double,
    velocityX: Double,
    velocityY: Double,
    velocityZ: Double,
    spriteProvider: SpriteProvider?,
) : SpellParticle(world, x, y, z, velocityX, velocityY, velocityZ, spriteProvider) {
  companion object {
    @Environment(EnvType.CLIENT)
    abstract class AbstractFlaskEffectParticleFactory : ModParticleFactory<SimpleParticleType>() {
      override fun createParticle(
          particleType: SimpleParticleType,
          world: ClientWorld,
          x: Double,
          y: Double,
          z: Double,
          velocityX: Double,
          velocityY: Double,
          velocityZ: Double,
      ) = FlaskEffectParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider)
    }

    @Environment(EnvType.CLIENT) class DefaultFactory() : AbstractFlaskEffectParticleFactory()

    @Environment(EnvType.CLIENT) class InstantFactory() : AbstractFlaskEffectParticleFactory()
  }

  init {
    this.maxAge *= FLASK_SPLASH_MULTIPLIER
  }
}
