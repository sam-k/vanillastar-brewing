package com.vanillastar.vsbrewing.block

import com.vanillastar.vsbrewing.block.entity.AbstractPotionCauldronBlockEntity
import com.vanillastar.vsbrewing.block.entity.PotionCauldronBlockEntity
import com.vanillastar.vsbrewing.utils.PotionContentType
import net.minecraft.block.Block
import net.minecraft.block.BlockEntityProvider
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.LeveledCauldronBlock
import net.minecraft.block.MapColor
import net.minecraft.item.ItemStack
import net.minecraft.particle.EntityEffectParticleEffect
import net.minecraft.particle.ParticleTypes
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.random.Random
import net.minecraft.world.World
import net.minecraft.world.WorldView
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

  override fun getPickStack(world: WorldView, pos: BlockPos, state: BlockState): ItemStack =
      Blocks.CAULDRON.getPickStack(world, pos, state)

  override fun randomDisplayTick(state: BlockState, world: World, pos: BlockPos, random: Random) {
    val renderData = world.getBlockEntityRenderData(pos)
    if (
        renderData !is AbstractPotionCauldronBlockEntity.RenderData ||
            renderData.contentType != PotionContentType.POTION
    ) {
      return
    }

    repeat(2) {
      world.addParticle(
          EntityEffectParticleEffect.create(ParticleTypes.ENTITY_EFFECT, renderData.color),
          pos.x.toDouble() + 0.4 + random.nextFloat() * 0.4,
          pos.y.toDouble() + 0.4 + random.nextFloat() * 0.3,
          pos.z.toDouble() + 0.4 + random.nextFloat() * 0.4,
          /* velocityX= */ 0.0,
          /* velocityY= */ 0.0,
          /* velocityZ= */ 0.0,
      )
    }
  }
}
