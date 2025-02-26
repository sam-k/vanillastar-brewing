package com.vanillastar.vsbrewing.mixin.block;

import static com.vanillastar.vsbrewing.block.ModBlocksKt.MOD_BLOCKS;
import static com.vanillastar.vsbrewing.tag.ModTagsKt.MOD_TAGS;

import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.BrewingStandBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BrewingStandBlock.class)
public abstract class BrewingStandBlockMixin extends BlockWithEntity {
  private BrewingStandBlockMixin(Settings settings) {
    super(settings);
  }

  @Override
  @Nullable
  public BlockState getPlacementState(@NotNull ItemPlacementContext ctx) {
    BlockState downState = ctx.getWorld().getBlockState(ctx.getBlockPos().down());
    if (!downState.isIn(MOD_TAGS.getBrewableCauldrons())) {
      return this.getDefaultState();
    }
    return MOD_BLOCKS.brewingCauldronStandBlock.getDefaultState();
  }

  @Override
  protected BlockState getStateForNeighborUpdate(
      BlockState state,
      Direction direction,
      BlockState neighborState,
      WorldAccess world,
      BlockPos pos,
      BlockPos neighborPos) {
    if (direction != Direction.DOWN || !neighborState.isIn(MOD_TAGS.getBrewableCauldrons())) {
      return super.getStateForNeighborUpdate(
          state, direction, neighborState, world, pos, neighborPos);
    }
    return MOD_BLOCKS.brewingCauldronStandBlock.getDefaultState();
  }
}
