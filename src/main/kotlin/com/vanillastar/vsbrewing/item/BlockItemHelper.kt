package com.vanillastar.vsbrewing.item

import com.vanillastar.vsbrewing.block.BottleBlock
import com.vanillastar.vsbrewing.block.MOD_BLOCKS
import net.minecraft.advancement.criterion.Criteria
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.ShapeContext
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.BlockStateComponent
import net.minecraft.fluid.Fluids
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.util.ActionResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.world.event.GameEvent

/**
 * Helper class to enable mixed-in block placement for [Item]'s that do not extend [BlockItem].
 *
 * This is mostly copied from block placement-related methods in [BlockItem].
 */
class BlockItemHelper(
    val block: Block,
    val placementStateProvider: (BlockState, ItemPlacementContext) -> BlockState,
) {
  private fun canPlace(context: ItemPlacementContext, state: BlockState): Boolean {
    val player = context.player
    val shapeContext = if (player == null) ShapeContext.absent() else ShapeContext.of(player)
    return state.canPlaceAt(context.world, context.blockPos) &&
        context.world.canPlace(state, context.blockPos, shapeContext)
  }

  private fun getPlacementState(context: ItemPlacementContext): BlockState? {
    var state = this.block.getPlacementState(context)
    if (state == null) {
      return null
    }

    state = this.placementStateProvider(state, context)
    return if (canPlace(context, state)) state else null
  }

  private fun place(context: ItemPlacementContext, state: BlockState) =
      context.world.setBlockState(context.blockPos, state, Block.NOTIFY_ALL_AND_REDRAW)

  private fun placeFromNbt(
      pos: BlockPos,
      world: World,
      stack: ItemStack,
      state: BlockState,
  ): BlockState {
    val stateComponent =
        stack.getOrDefault(DataComponentTypes.BLOCK_STATE, BlockStateComponent.DEFAULT)
    if (stateComponent.isEmpty) {
      return state
    }

    val newState = stateComponent.applyToState(state)
    if (newState !== state) {
      world.setBlockState(pos, newState, Block.NOTIFY_LISTENERS)
    }
    return newState
  }

  private fun copyComponentsToBlockEntity(world: World, pos: BlockPos, stack: ItemStack) {
    val blockEntity = world.getBlockEntity(pos)
    if (blockEntity != null) {
      blockEntity.readComponents(stack)
      blockEntity.markDirty()
    }
  }

  fun place(context: ItemUsageContext) = this.place(ItemPlacementContext(context))

  fun place(context: ItemPlacementContext): ActionResult {
    val world: World = context.world
    if (!this.block.isEnabled(world.enabledFeatures) || !context.canPlace()) {
      return ActionResult.FAIL
    }

    val pos = context.blockPos
    val state = this.getPlacementState(context)
    if (state == null) {
      return ActionResult.FAIL
    }

    if (!this.place(context, state)) {
      return ActionResult.FAIL
    }

    val stack = context.stack
    val player = context.player
    var newState = world.getBlockState(pos)
    if (newState.isOf(state.block)) {
      newState = placeFromNbt(pos, world, stack, newState)
      BlockItem.writeNbtToBlockEntity(world, player, pos, stack)
      this.copyComponentsToBlockEntity(world, pos, stack)
      newState.block.onPlaced(world, pos, newState, player, stack)
      if (player is ServerPlayerEntity) {
        Criteria.PLACED_BLOCK.trigger(player, pos, stack)
      }
    }

    val soundGroup = newState.soundGroup
    world.playSound(
        player,
        pos,
        soundGroup.placeSound,
        SoundCategory.BLOCKS,
        (soundGroup.getVolume() + 1.0f) / 2.0f,
        soundGroup.getPitch() * 0.8f,
    )
    world.emitGameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Emitter.of(player, newState))
    stack.decrementUnlessCreative(1, player)

    return ActionResult.success(world.isClient)
  }
}

@JvmField
val PLACEABLE_BOTTLE_BLOCK_ITEM_HELPER =
    BlockItemHelper(MOD_BLOCKS.bottleBlock) { state, placementContext ->
      val world = placementContext.world
      val pos = placementContext.blockPos
      val prevState = world.getBlockState(pos)
      state
          .with(
              BottleBlock.COUNT,
              if (prevState.isOf(MOD_BLOCKS.bottleBlock) && prevState.contains(BottleBlock.COUNT)) {
                1 + prevState.get(BottleBlock.COUNT)
              } else {
                BottleBlock.MIN_COUNT
              },
          )
          .with(BottleBlock.WATERLOGGED, world.getFluidState(pos).fluid.matchesType(Fluids.WATER))
    }
