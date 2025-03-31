package com.vanillastar.vsbrewing.networking

import com.vanillastar.vsbrewing.block.entity.FLASK_SPLASH_MULTIPLIER
import com.vanillastar.vsbrewing.item.MOD_ITEMS
import com.vanillastar.vsbrewing.particle.MOD_PARTICLE_TYPES
import com.vanillastar.vsbrewing.utils.getModIdentifier
import kotlin.math.cos
import kotlin.math.sin
import net.minecraft.client.render.WorldRenderer
import net.minecraft.item.Items
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload
import net.minecraft.particle.ItemStackParticleEffect
import net.minecraft.particle.ParticleEffect
import net.minecraft.particle.ParticleType
import net.minecraft.particle.ParticleTypes
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ColorHelper.Argb
import net.minecraft.util.math.Vec3d
import net.minecraft.world.WorldEvents

/** Networking payload for updating clients on data related to rendering a thrown potion. */
data class ThrownPotionPayload(
    val packedPos: Long,
    val type: String,
    val isInstant: Boolean,
    val color: Int,
) : ModNetworkingPayload.Server<ThrownPotionPayload> {
  enum class Type {
    SPLASH_POTION,
    LINGERING_POTION,
    SPLASH_FLASK,
    LINGERING_FLASK,
  }

  companion object : ModNetworkingPayload.ServerCompanion<ThrownPotionPayload> {
    val THROWN_POTION_PACKET_ID = getModIdentifier("thrown_potion")

    override val id: CustomPayload.Id<ThrownPotionPayload> =
        CustomPayload.Id(THROWN_POTION_PACKET_ID)

    override val codec: PacketCodec<RegistryByteBuf, ThrownPotionPayload> =
        PacketCodec.tuple(
            PacketCodecs.VAR_LONG,
            ThrownPotionPayload::packedPos,
            PacketCodecs.STRING,
            ThrownPotionPayload::type,
            PacketCodecs.BOOL,
            ThrownPotionPayload::isInstant,
            PacketCodecs.INTEGER,
            ThrownPotionPayload::color,
            ::ThrownPotionPayload,
        )

    /**
     * This is mostly copied from [WorldRenderer.processWorldEvent], cases
     * [WorldEvents.SPLASH_POTION_SPLASHED] and [WorldEvents.INSTANT_SPLASH_POTION_SPLASHED], except
     */
    override val callback: ModNetworkingClientCallback<ThrownPotionPayload> = { payload, context ->
      {
        val type = Type.valueOf(payload.type)
        val pos = BlockPos.fromLong(payload.packedPos)
        val vec = Vec3d.ofBottomCenter(pos)

        val world = context.client().world
        val worldRenderer = context.client().worldRenderer

        // Spawn item break particles.
        val stack =
            when (type) {
              Type.SPLASH_POTION -> Items.SPLASH_POTION
              Type.LINGERING_POTION -> Items.LINGERING_POTION
              Type.SPLASH_FLASK -> MOD_ITEMS.splashPotionFlaskItem
              Type.LINGERING_FLASK -> MOD_ITEMS.lingeringPotionFlaskItem
            }.defaultStack
        repeat(8) {
          world?.addParticle(
              ItemStackParticleEffect(ParticleTypes.ITEM, stack),
              vec.x,
              vec.y,
              vec.z,
              world.random.nextGaussian() * 0.15,
              world.random.nextDouble() * 0.2,
              world.random.nextGaussian() * 0.15,
          )
        }

        // Spawn potion effect particles.
        var numParticles: Int
        var particleType: ParticleType<out ParticleEffect>
        when (type) {
          Type.SPLASH_POTION,
          Type.LINGERING_POTION -> {
            numParticles = 100
            particleType =
                if (payload.isInstant) {
                  ParticleTypes.INSTANT_EFFECT
                } else {
                  ParticleTypes.EFFECT
                }
          }
          Type.SPLASH_FLASK,
          Type.LINGERING_FLASK -> {
            numParticles = 100 * FLASK_SPLASH_MULTIPLIER * FLASK_SPLASH_MULTIPLIER
            particleType =
                if (payload.isInstant) {
                  MOD_PARTICLE_TYPES.flaskInstantEffectParticleType
                } else {
                  MOD_PARTICLE_TYPES.flaskEffectParticleType
                }
          }
        }
        repeat(numParticles) {
          val velocity = (world?.random?.nextDouble() ?: 0.0) * 4.0
          val angleXZ = (world?.random?.nextDouble() ?: 0.0) * Math.PI * 2.0
          val velocityX = cos(angleXZ) * velocity
          val velocityY = 0.01 + (world?.random?.nextDouble() ?: 0.0) * 0.5
          val velocityZ = sin(angleXZ) * velocity
          val particle =
              worldRenderer.spawnParticle(
                  particleType,
                  particleType.shouldAlwaysSpawn(),
                  /* canSpawnOnMinimal= */ false,
                  vec.x + velocityX * 0.1,
                  vec.y + 0.3,
                  vec.z + velocityZ * 0.1,
                  velocityX,
                  velocityY,
                  velocityZ,
              )
          val colorModifier = (0.75f + (world?.random?.nextFloat() ?: 0.0f) * 0.25f) / 255.0f
          particle?.setColor(
              colorModifier * Argb.getRed(payload.color),
              colorModifier * Argb.getGreen(payload.color),
              colorModifier * Argb.getBlue(payload.color),
          )
          particle?.move(velocity.toFloat())
        }

        world?.playSoundAtBlockCenter(
            pos,
            SoundEvents.ENTITY_SPLASH_POTION_BREAK,
            SoundCategory.NEUTRAL,
            /* volume= */ 1.0f,
            /* pitch= */ world.random.nextFloat() * 0.1f + 0.9f,
            /* useDistance= */ false,
        )
      }
    }
  }

  override val companion = Companion

  override fun getId(): CustomPayload.Id<ThrownPotionPayload> = companion.id
}
