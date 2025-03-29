package com.vanillastar.vsbrewing.render

import com.vanillastar.vsbrewing.block.entity.PotionCauldronPreviewBlockEntity
import com.vanillastar.vsbrewing.potion.MILK_POTION_ID
import com.vanillastar.vsbrewing.potion.potionContentsMatchId
import com.vanillastar.vsbrewing.utils.getModIdentifier
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.block.LeveledCauldronBlock
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.render.entity.model.EntityModelLayer
import net.minecraft.util.math.Direction

val POTION_CAULDRON_PREVIEW_CONTENT_MODEL_LAYERS =
    (LeveledCauldronBlock.LEVEL.values).associate {
      it to EntityModelLayer(getModIdentifier("potion_cauldron_preview_level${it}"), "root")
    }

@Environment(EnvType.CLIENT)
class PotionCauldronPreviewBlockEntityRenderer(context: BlockEntityRendererFactory.Context) :
    LeveledBlockEntityRenderer<PotionCauldronPreviewBlockEntity>(context) {
  companion object {
    fun getContentTexturedModelData(level: Int) =
        getContentTexturedModelData(
            level,
            LeveledCauldronBlock.LEVEL,
            setOf(Direction.UP, Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST),
            sizeXZ = 12.0f,
            sizeY =
                when (level) {
                  1 -> 5.0f
                  2 -> 8.0f
                  3 -> 11.0f
                  else -> 0.0f
                },
            offsetXZ = 2.0f,
            offsetY = 4.0f,
        )
  }

  override fun getLeveledEntityModelLayerMap() = POTION_CAULDRON_PREVIEW_CONTENT_MODEL_LAYERS

  override fun getLevel(entity: PotionCauldronPreviewBlockEntity) = entity.forcedLevel ?: 0

  override fun getSpriteId(entity: PotionCauldronPreviewBlockEntity) =
      when {
        potionContentsMatchId(entity.potionContents, MILK_POTION_ID) -> MILK_SPRITE_ID
        else -> WATER_SPRITE_ID
      }

  override fun getColor(entity: PotionCauldronPreviewBlockEntity) = entity.renderData.color
}
