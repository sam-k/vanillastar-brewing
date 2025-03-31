package com.vanillastar.vsbrewing.block

import com.mojang.serialization.MapCodec
import com.vanillastar.vsbrewing.block.entity.BottleBlockEntity
import com.vanillastar.vsbrewing.block.entity.MOD_BLOCK_ENTITIES
import com.vanillastar.vsbrewing.tag.MOD_TAGS
import com.vanillastar.vsbrewing.utils.getModIdentifier
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
import net.minecraft.item.ItemPlacementContext
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
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.minecraft.world.WorldAccess

val BOTTLE_BLOCK_METADATA =
    ModBlockMetadata("bottle") {
      it.strength(0.1f)
          .pistonBehavior(PistonBehavior.DESTROY)
          .sounds(BlockSoundGroup.COPPER_BULB)
          .instrument(NoteBlockInstrument.HAT)
          .nonOpaque()
    }

/** [Block] for placed bottles of any type. */
class BottleBlock(settings: Settings) : BlockWithEntity(settings), Waterloggable {
  companion object {
    private val CODEC: MapCodec<BottleBlock> = createCodec(::BottleBlock)
    private val SHAPE_BY_COUNT =
        mapOf(
            1 to
                createCuboidShape(
                    /* minX= */ 5.0,
                    /* minY= */ 0.0,
                    /* minZ= */ 5.0,
                    /* maxX= */ 10.0,
                    /* maxY= */ 8.0,
                    /* maxZ= */ 10.0,
                ),
            2 to
                createCuboidShape(
                    /* minX= */ 2.0,
                    /* minY= */ 0.0,
                    /* minZ= */ 2.0,
                    /* maxX= */ 13.0,
                    /* maxY= */ 8.0,
                    /* maxZ= */ 13.0,
                ),
            3 to
                createCuboidShape(
                    /* minX= */ 2.0,
                    /* minY= */ 0.0,
                    /* minZ= */ 2.0,
                    /* maxX= */ 14.0,
                    /* maxY= */ 8.0,
                    /* maxZ= */ 14.0,
                ),
            4 to
                createCuboidShape(
                    /* minX= */ 2.0,
                    /* minY= */ 0.0,
                    /* minZ= */ 2.0,
                    /* maxX= */ 14.0,
                    /* maxY= */ 8.0,
                    /* maxZ= */ 14.0,
                ),
        )
    private val DYNAMIC_DROP_ID = getModIdentifier("bottle")

    const val MIN_COUNT = 1
    const val MAX_COUNT = 4

    val COUNT: IntProperty = IntProperty.of("count", MIN_COUNT, MAX_COUNT)
    val WATERLOGGED: BooleanProperty = Properties.WATERLOGGED
  }

  init {
    this.defaultState = this.stateManager.getDefaultState().with(COUNT, 2).with(WATERLOGGED, false)
  }

  override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
    builder.add(COUNT, WATERLOGGED)
  }

  override fun getCodec() = CODEC

  override fun createBlockEntity(pos: BlockPos, state: BlockState) = BottleBlockEntity(pos, state)

  override fun getRenderType(state: BlockState) = BlockRenderType.MODEL

  override fun getOutlineShape(
      state: BlockState,
      world: BlockView,
      pos: BlockPos,
      context: ShapeContext,
  ): VoxelShape = SHAPE_BY_COUNT[state.getOrEmpty(COUNT).orElse(MIN_COUNT)]!!

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
    world.getBlockEntity(pos, MOD_BLOCK_ENTITIES.bottleBlockEntityType).ifPresent {
      if (it.canInsert(stack)) {
        it.insert(stack.copyWithCount(1))
        it.markDirty()
      }
    }
  }

  override fun canReplace(state: BlockState, context: ItemPlacementContext) =
      if (
          !context.stack.isIn(MOD_TAGS.placeableBottles) ||
              (context.shouldCancelInteraction() &&
                  !context.stack.isIn(MOD_TAGS.placeableBottlesWithSneaking)) ||
              state.getOrEmpty(COUNT).orElse(0) >= MAX_COUNT
      ) {
        super.canReplace(state, context)
      } else true

  override fun getDroppedStacks(
      state: BlockState,
      builder: LootContextParameterSet.Builder,
  ): List<ItemStack> {
    val blockEntity = builder.getOptional(LootContextParameters.BLOCK_ENTITY)
    if (blockEntity is BottleBlockEntity) {
      builder.addDynamicDrop(DYNAMIC_DROP_ID) {
        for (stack in blockEntity.iterateItems()) {
          it.accept(stack)
        }
      }
    }
    return super.getDroppedStacks(state, builder)
  }

  override fun hasComparatorOutput(state: BlockState) = true

  override fun getComparatorOutput(state: BlockState, world: World, pos: BlockPos): Int =
      state.getOrEmpty(COUNT).orElse(0)

  override fun canPathfindThrough(state: BlockState, type: NavigationType) = false
}
