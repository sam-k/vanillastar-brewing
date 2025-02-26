package com.vanillastar.vsbrewing.block

import com.vanillastar.vsbrewing.block.entity.PotionCauldronBlockEntity
import net.minecraft.block.*
import net.minecraft.state.StateManager
import net.minecraft.state.property.IntProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.math.BlockPos
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

  override fun getComparatorOutput(state: BlockState, world: World, pos: BlockPos): Int =
      state.get(LEVEL)
}
