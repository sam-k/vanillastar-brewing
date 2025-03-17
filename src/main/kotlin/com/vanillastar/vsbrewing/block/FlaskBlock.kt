package com.vanillastar.vsbrewing.block

import com.mojang.serialization.MapCodec
import com.vanillastar.vsbrewing.block.entity.FlaskBlockEntity
import kotlin.jvm.optionals.getOrDefault
import net.minecraft.block.*
import net.minecraft.block.enums.NoteBlockInstrument
import net.minecraft.entity.ai.pathing.NavigationType
import net.minecraft.fluid.FluidState
import net.minecraft.fluid.Fluids
import net.minecraft.item.ItemPlacementContext
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.IntProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.minecraft.world.WorldAccess

val FLASK_BLOCK_METADATA =
    ModBlockMetadata("flask") {
      it.strength(0.3F)
          .sounds(BlockSoundGroup.GLASS)
          .instrument(NoteBlockInstrument.HAT)
          .nonOpaque()
    }

open class FlaskBlock(settings: Settings) : BlockWithEntity(settings), Waterloggable {
  companion object {
    private val CODEC: MapCodec<FlaskBlock> = createCodec(::FlaskBlock)

    private val SHAPE: VoxelShape =
        VoxelShapes.union(
            createCuboidShape(
                /* minX= */ 2.0,
                /* minY= */ 0.0,
                /* minZ= */ 2.0,
                /* maxX= */ 14.0,
                /* maxY= */ 15.0,
                /* maxZ= */ 14.0,
            ),
            createCuboidShape(
                /* minX= */ 5.0,
                /* minY= */ 13.0,
                /* minZ= */ 5.0,
                /* maxX= */ 11.0,
                /* maxY= */ 16.0,
                /* maxZ= */ 11.0,
            ),
        )

    internal val LEVEL: IntProperty = IntProperty.of("level", /* min= */ 0, /* max= */ 3)
    internal val WATERLOGGED: BooleanProperty = Properties.WATERLOGGED
  }

  init {
    this.defaultState = this.stateManager.getDefaultState().with(LEVEL, 0).with(WATERLOGGED, false)
  }

  override fun getCodec() = CODEC

  override fun createBlockEntity(pos: BlockPos, state: BlockState) = FlaskBlockEntity(pos, state)

  override fun getRenderType(state: BlockState) = BlockRenderType.MODEL

  override fun getOutlineShape(
      state: BlockState,
      world: BlockView,
      pos: BlockPos,
      context: ShapeContext,
  ) = SHAPE

  override fun getPlacementState(context: ItemPlacementContext): BlockState? =
      super.getPlacementState(context)
          ?.with(
              WATERLOGGED,
              context.world.getFluidState(context.blockPos).fluid.matchesType(Fluids.WATER),
          )

  override fun getFluidState(state: BlockState): FluidState =
      if (state.getOrEmpty(WATERLOGGED).getOrDefault(false)) {
        Fluids.WATER.getStill(false)
      } else {
        super.getFluidState(state)
      }

  override fun getStateForNeighborUpdate(
      state: BlockState,
      direction: Direction,
      neighborState: BlockState,
      world: WorldAccess,
      pos: BlockPos,
      neighborPos: BlockPos,
  ): BlockState {
    if (state.getOrEmpty(WATERLOGGED).getOrDefault(false)) {
      world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world))
    }
    return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos)
  }

  override fun canPathfindThrough(state: BlockState, type: NavigationType) = false

  override fun getComparatorOutput(state: BlockState, world: World, pos: BlockPos): Int =
      state.getOrEmpty(LEVEL).getOrDefault(0)

  override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
    builder.add(LEVEL, WATERLOGGED)
  }
}
