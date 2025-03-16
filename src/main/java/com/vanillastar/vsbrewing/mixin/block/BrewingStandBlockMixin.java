package com.vanillastar.vsbrewing.mixin.block;

import static com.vanillastar.vsbrewing.block.ModBlocksKt.BREWING_STAND_IS_ON_CAULDRON;
import static com.vanillastar.vsbrewing.tag.ModTagsKt.MOD_TAGS;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.BrewingStandBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BrewingStandBlock.class)
public abstract class BrewingStandBlockMixin extends BlockWithEntity {
  @Unique
  @Final
  private static VoxelShape BREWING_CAULDRON_SHAPE =
      createCuboidShape(6.0, -11.0, 6.0, 10.0, 14.0, 10.0);

  private BrewingStandBlockMixin(Settings settings) {
    super(settings);
  }

  @Override
  public BlockState getPlacementState(@NotNull ItemPlacementContext ctx) {
    BlockState state = super.getPlacementState(ctx);
    return state == null
        ? null
        : state.with(
            BREWING_STAND_IS_ON_CAULDRON,
            ctx.getWorld()
                .getBlockState(ctx.getBlockPos().down())
                .isIn(MOD_TAGS.brewableCauldrons));
  }

  @Override
  protected BlockState getStateForNeighborUpdate(
      BlockState state,
      Direction direction,
      BlockState neighborState,
      WorldAccess world,
      BlockPos pos,
      BlockPos neighborPos) {
    state =
        super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    return direction == Direction.DOWN
        ? state.with(BREWING_STAND_IS_ON_CAULDRON, neighborState.isIn(MOD_TAGS.brewableCauldrons))
        : state;
  }

  @Inject(method = "<init>(Lnet/minecraft/block/AbstractBlock$Settings;)V", at = @At("TAIL"))
  private void brewingCauldronConstructor(Settings settings, CallbackInfo ci) {
    this.setDefaultState(this.getDefaultState().with(BREWING_STAND_IS_ON_CAULDRON, false));
  }

  @Inject(
      method = "appendProperties(Lnet/minecraft/state/StateManager$Builder;)V",
      at = @At("HEAD"))
  private void appendBrewingCauldronProperties(
      StateManager.@NotNull Builder<Block, BlockState> builder, CallbackInfo ci) {
    builder.add(BREWING_STAND_IS_ON_CAULDRON);
  }

  @Inject(
      method =
          "getOutlineShape(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/ShapeContext;)Lnet/minecraft/util/shape/VoxelShape;",
      at = @At("HEAD"),
      cancellable = true)
  private void getOutlineShape(
      BlockState state,
      @NotNull BlockView world,
      @NotNull BlockPos pos,
      ShapeContext context,
      CallbackInfoReturnable<VoxelShape> cir) {
    if (!world.getBlockState(pos.down()).isIn(MOD_TAGS.brewableCauldrons)) {
      return;
    }
    cir.setReturnValue(BREWING_CAULDRON_SHAPE);
    cir.cancel();
  }
}
