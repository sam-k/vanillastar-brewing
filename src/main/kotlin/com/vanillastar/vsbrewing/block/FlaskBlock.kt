package com.vanillastar.vsbrewing.block

import com.mojang.serialization.MapCodec
import com.vanillastar.vsbrewing.block.entity.FlaskBlockEntity
import com.vanillastar.vsbrewing.block.entity.MOD_BLOCK_ENTITIES
import com.vanillastar.vsbrewing.utils.getModIdentifier
import kotlin.jvm.optionals.getOrNull
import net.minecraft.block.Block
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.ShapeContext
import net.minecraft.block.Waterloggable
import net.minecraft.block.enums.NoteBlockInstrument
import net.minecraft.block.piston.PistonBehavior
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.ai.pathing.NavigationType
import net.minecraft.fluid.FluidState
import net.minecraft.fluid.Fluids
import net.minecraft.item.ItemStack
import net.minecraft.loot.context.LootContextParameterSet
import net.minecraft.loot.context.LootContextParameters
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
      it.strength(0.1f)
          .pistonBehavior(PistonBehavior.DESTROY)
          .sounds(BlockSoundGroup.COPPER_BULB)
          .instrument(NoteBlockInstrument.HAT)
          .nonOpaque()
    }

/** [Block] for a placed flask of any kind. */
class FlaskBlock(settings: Settings) : BlockWithEntity(settings), Waterloggable {
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
    private val DYNAMIC_DROP_ID = getModIdentifier("flask")

    const val MIN_LEVEL = 0
    const val MAX_LEVEL = 4

    val LEVEL: IntProperty = IntProperty.of("level", MIN_LEVEL, MAX_LEVEL)
    val WATERLOGGED: BooleanProperty = Properties.WATERLOGGED
  }

  init {
    this.defaultState =
        this.stateManager.getDefaultState().with(LEVEL, MIN_LEVEL).with(WATERLOGGED, false)
  }

  override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
    builder.add(LEVEL, WATERLOGGED)
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

  override fun getFluidState(state: BlockState): FluidState =
      if (state.getOrEmpty(WATERLOGGED).orElse(false)) {
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
    if (state.getOrEmpty(WATERLOGGED).orElse(false)) {
      world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world))
    }
    return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos)
  }

  override fun onPlaced(
      world: World,
      pos: BlockPos,
      state: BlockState,
      placer: LivingEntity?,
      stack: ItemStack,
  ) {
    super.onPlaced(world, pos, state, placer, stack)
    val blockEntity = world.getBlockEntity(pos, MOD_BLOCK_ENTITIES.flaskBlockEntityType).getOrNull()
    blockEntity?.item = stack.copyWithCount(1)
    blockEntity?.markDirty()
  }

  override fun getDroppedStacks(
      state: BlockState,
      builder: LootContextParameterSet.Builder,
  ): List<ItemStack> {
    val blockEntity = builder.getOptional(LootContextParameters.BLOCK_ENTITY)
    if (blockEntity is FlaskBlockEntity) {
      builder.addDynamicDrop(DYNAMIC_DROP_ID) { it.accept(blockEntity.item) }
    }
    return super.getDroppedStacks(state, builder)
  }

  override fun hasComparatorOutput(state: BlockState) = true

  override fun getComparatorOutput(state: BlockState, world: World, pos: BlockPos): Int =
      state.getOrEmpty(LEVEL).orElse(0)

  override fun canPathfindThrough(state: BlockState, type: NavigationType) = false
}
