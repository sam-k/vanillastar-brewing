package com.vanillastar.vsbrewing.block

import com.vanillastar.vsbrewing.block.entity.PotionCauldronBlockEntity
import net.minecraft.block.Block
import net.minecraft.block.BlockEntityProvider
import net.minecraft.block.BlockState
import net.minecraft.block.LeveledCauldronBlock
import net.minecraft.block.MapColor
import net.minecraft.particle.EntityEffectParticleEffect
import net.minecraft.particle.ParticleTypes
import net.minecraft.state.StateManager
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.random.Random
import net.minecraft.world.World
import net.minecraft.world.biome.Biome

val POTION_CAULDRON_BLOCK_METADATA =
    ModBlockMetadata("potion_cauldron") {
      it.mapColor(MapColor.STONE_GRAY).requiresTool().strength(2.0f)
    }

/**
 * [Block] for a potion-filled cauldron.
 *
 * Water-filled cauldrons technically count as a potion cauldron but remain as the vanilla
 * [LeveledCauldronBlock] for compatibility.
 */
open class PotionCauldronBlock(settings: Settings) :
    LeveledCauldronBlock(
        Biome.Precipitation.NONE,
        MOD_CAULDRON_BEHAVIORS.potionCauldronBehavior,
        settings,
    ),
    BlockEntityProvider {
  override fun createBlockEntity(pos: BlockPos, state: BlockState) =
      PotionCauldronBlockEntity(pos, state)

  override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
    builder.add(LEVEL)
  }

  override fun isFull(state: BlockState) = state.get(LEVEL) == 3

  override fun getFluidHeight(state: BlockState) = (6 + 3 * state.get(LEVEL)) / 16.0

  override fun randomDisplayTick(state: BlockState, world: World, pos: BlockPos, random: Random) {
    val potionColor = world.getBlockEntityRenderData(pos)
    val particleEffect =
        if (potionColor is Int) {
          EntityEffectParticleEffect.create(ParticleTypes.ENTITY_EFFECT, potionColor)
        } else {
          ParticleTypes.EFFECT
        }
    repeat(2) {
      world.addParticle(
          particleEffect,
          pos.x.toDouble() + 0.4 + random.nextFloat() * 0.4,
          pos.y.toDouble() + 0.4 + random.nextFloat() * 0.3,
          pos.z.toDouble() + 0.4 + random.nextFloat() * 0.4,
          /* velocityX= */ 0.0,
          /* velocityY= */ 0.0,
          /* velocityZ= */ 0.0,
      )
    }
  }

  override fun getComparatorOutput(state: BlockState, world: World, pos: BlockPos): Int =
      state.get(LEVEL)
}
