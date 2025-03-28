package com.vanillastar.vsbrewing.block

import net.minecraft.block.BlockState
import net.minecraft.block.MapColor
import net.minecraft.block.ShapeContext
import net.minecraft.util.function.BooleanBiFunction
import net.minecraft.util.math.BlockPos
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView

val POTION_CAULDRON_PREVIEW_BLOCK_METADATA =
    ModBlockMetadata("potion_cauldron_preview") {
      it.mapColor(MapColor.STONE_GRAY)
          .strength(/* hardness= */ -1.0f, /* resistance= */ 3600000.0f)
          .dropsNothing()
    }

/** [PotionCauldronBlock] with two cauldron sides removed to show the potion contents inside. */
class PotionCauldronPreviewBlock(settings: Settings) : PotionCauldronBlock(settings) {
  private companion object {
    val RAYCAST_SHAPE: VoxelShape =
        createCuboidShape(
            /* minX= */ 2.0,
            /* minY= */ 4.0,
            /* minZ= */ 0.0,
            /* maxX= */ 16.0,
            /* maxY= */ 16.0,
            /* maxZ= */ 14.0,
        )
    val OUTLINE_SHAPE: VoxelShape =
        VoxelShapes.combineAndSimplify(
            VoxelShapes.fullCube(),
            VoxelShapes.union(
                // Negative space below the cauldron along the X-axis.
                createCuboidShape(
                    /* minX= */ 0.0,
                    /* minY= */ 0.0,
                    /* minZ= */ 4.0,
                    /* maxX= */ 16.0,
                    /* maxY= */ 3.0,
                    /* maxZ= */ 12.0,
                ),
                // Negative space below the cauldron along the Z-axis.
                createCuboidShape(
                    /* minX= */ 4.0,
                    /* minY= */ 0.0,
                    /* minZ= */ 0.0,
                    /* maxX= */ 12.0,
                    /* maxY= */ 3.0,
                    /* maxZ= */ 16.0,
                ),
                // Negative space below the cauldron between the four legs.
                createCuboidShape(
                    /* minX= */ 2.0,
                    /* minY= */ 0.0,
                    /* minZ= */ 2.0,
                    /* maxX= */ 14.0,
                    /* maxY= */ 3.0,
                    /* maxZ= */ 14.0,
                ),
                RAYCAST_SHAPE,
            ),
            BooleanBiFunction.ONLY_FIRST,
        )
  }

  override fun getRaycastShape(state: BlockState, world: BlockView, pos: BlockPos) = RAYCAST_SHAPE

  override fun getOutlineShape(
      state: BlockState,
      world: BlockView,
      pos: BlockPos,
      context: ShapeContext,
  ) = OUTLINE_SHAPE
}
