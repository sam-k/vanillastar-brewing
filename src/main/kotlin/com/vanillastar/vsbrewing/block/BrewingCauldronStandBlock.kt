package com.vanillastar.vsbrewing.block

import com.mojang.serialization.MapCodec
import com.vanillastar.vsbrewing.block.entity.BrewingCauldronStandBlockEntity
import com.vanillastar.vsbrewing.block.entity.MOD_BLOCK_ENTITIES
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.ai.pathing.NavigationType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.particle.ParticleTypes
import net.minecraft.screen.ScreenHandler
import net.minecraft.stat.Stats
import net.minecraft.util.ActionResult
import net.minecraft.util.ItemScatterer
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.random.Random
import net.minecraft.util.shape.VoxelShape
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.minecraft.world.WorldAccess

val BREWING_CAULDRON_STAND_BLOCK_METADATA =
    ModBlockMetadata("brewing_cauldron_stand") {
      it.mapColor(MapColor.IRON_GRAY).requiresTool().strength(0.5f).nonOpaque().luminance { 1 }
    }

class BrewingCauldronStandBlock(settings: Settings) : BlockWithEntity(settings) {
  private companion object {
    val CODEC: MapCodec<BrewingCauldronStandBlock> = createCodec(::BrewingCauldronStandBlock)
    val SHAPE: VoxelShape =
        createCuboidShape(
            /* minX= */ 7.0,
            /* minY= */ -11.0,
            /* minZ= */ 7.0,
            /* maxX= */ 9.0,
            /* maxY= */ 14.0,
            /* maxZ= */ 9.0,
        )
  }

  override fun getCodec() = CODEC

  override fun createBlockEntity(pos: BlockPos, state: BlockState) =
      BrewingCauldronStandBlockEntity(pos, state)

  override fun getRenderType(state: BlockState) = BlockRenderType.MODEL

  override fun getOutlineShape(
      state: BlockState,
      world: BlockView,
      pos: BlockPos,
      context: ShapeContext,
  ) = SHAPE

  override fun <TBlockEntity : BlockEntity> getTicker(
      world: World,
      state: BlockState,
      type: BlockEntityType<TBlockEntity>,
  ): BlockEntityTicker<TBlockEntity>? {
    if (world.isClient) {
      return null
    }
    return validateTicker(
        type,
        MOD_BLOCK_ENTITIES.brewingCauldronStandBlockEntityType,
        BrewingCauldronStandBlockEntity::tick,
    )
  }

  override fun onUse(
      state: BlockState,
      world: World,
      pos: BlockPos,
      player: PlayerEntity,
      hit: BlockHitResult,
  ): ActionResult {
    if (world.isClient) {
      return ActionResult.SUCCESS
    }

    val blockEntity =
        world.getBlockEntity(pos, MOD_BLOCK_ENTITIES.brewingCauldronStandBlockEntityType)
    if (blockEntity.isPresent) {
      player.openHandledScreen(blockEntity.get())
      player.incrementStat(Stats.INTERACT_WITH_BREWINGSTAND)
    }
    return ActionResult.CONSUME
  }

  override fun onStateReplaced(
      state: BlockState,
      world: World,
      pos: BlockPos,
      newState: BlockState,
      moved: Boolean,
  ) {
    ItemScatterer.onStateReplaced(state, newState, world, pos)
    super.onStateReplaced(state, world, pos, newState, moved)
  }

  override fun getStateForNeighborUpdate(
      state: BlockState,
      direction: Direction,
      neighborState: BlockState,
      world: WorldAccess,
      pos: BlockPos,
      neighborPos: BlockPos,
  ): BlockState {
    if (
        direction != Direction.DOWN ||
            neighborState.isOf(Blocks.CAULDRON) ||
            neighborState.isOf(Blocks.WATER_CAULDRON) ||
            neighborState.isOf(MOD_BLOCKS.potionCauldronBlock)
    ) {
      return super.getStateForNeighborUpdate(
          state,
          direction,
          neighborState,
          world,
          pos,
          neighborPos,
      )
    }
    return Blocks.BREWING_STAND.defaultState
  }

  override fun afterBreak(
      world: World,
      player: PlayerEntity,
      pos: BlockPos,
      state: BlockState,
      blockEntity: BlockEntity?,
      tool: ItemStack,
  ) {
    player.incrementStat(Stats.MINED.getOrCreateStat(Blocks.BREWING_STAND))
    player.addExhaustion(0.005f)
    dropStacks(state, world, pos, blockEntity, player, tool)
  }

  override fun randomDisplayTick(state: BlockState, world: World, pos: BlockPos, random: Random) {
    world.addParticle(
        ParticleTypes.SMOKE,
        pos.x.toDouble() + 0.4 + random.nextFloat().toDouble() * 0.2,
        pos.y.toDouble() + 0.7 + random.nextFloat().toDouble() * 0.3,
        pos.z.toDouble() + 0.4 + random.nextFloat().toDouble() * 0.2,
        /* velocityX= */ 0.0,
        /* velocityY= */ 0.0,
        /* velocityZ= */ 0.0,
    )
  }

  override fun hasComparatorOutput(state: BlockState) = true

  override fun getComparatorOutput(state: BlockState, world: World, pos: BlockPos) =
      ScreenHandler.calculateComparatorOutput(world.getBlockEntity(pos))

  override fun canPathfindThrough(state: BlockState, type: NavigationType) = false
}
