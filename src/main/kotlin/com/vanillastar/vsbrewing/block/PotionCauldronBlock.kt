package com.vanillastar.vsbrewing.block

import com.vanillastar.vsbrewing.block.entity.PotionCauldronBlockEntity
import net.minecraft.block.*
import net.minecraft.particle.EntityEffectParticleEffect
import net.minecraft.particle.ParticleTypes
import net.minecraft.state.StateManager
import net.minecraft.state.property.IntProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.random.Random
import net.minecraft.world.World
import net.minecraft.world.biome.Biome

val POTION_CAULDRON_BLOCK_METADATA =
    ModBlockMetadata("potion_cauldron") {
      it.mapColor(MapColor.STONE_GRAY).requiresTool().strength(2.0f)
    }

class PotionCauldronBlock(settings: Settings) :
    LeveledCauldronBlock(
        Biome.Precipitation.NONE,
        MOD_CAULDRON_BEHAVIORS.potionCauldronBehavior,
        settings,
    ),
    BlockEntityProvider {
  companion object {
    val LEVEL: IntProperty = Properties.LEVEL_3
  }

  override fun createBlockEntity(pos: BlockPos, state: BlockState) =
      PotionCauldronBlockEntity(pos, state)

  override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
    builder.add(LEVEL)
  }

  override fun isFull(state: BlockState) = state.get(LEVEL) == 3

  override fun getFluidHeight(state: BlockState) = (6 + 3 * state.get(LEVEL)) / 16.0

  override fun randomDisplayTick(state: BlockState, world: World, pos: BlockPos, random: Random) {
    var potionColor = world.getBlockEntityRenderData(pos) as? Int ?: 0
    repeat(2) {
      world.addParticle(
          EntityEffectParticleEffect.create(ParticleTypes.ENTITY_EFFECT, potionColor),
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
