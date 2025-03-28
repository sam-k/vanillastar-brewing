package com.vanillastar.vsbrewing.render

import com.vanillastar.vsbrewing.block.FlaskBlock
import com.vanillastar.vsbrewing.block.entity.FlaskBlockEntity
import com.vanillastar.vsbrewing.item.MOD_ITEMS
import com.vanillastar.vsbrewing.potion.MILK_POTION_ID
import com.vanillastar.vsbrewing.potion.potionContentsMatchId
import com.vanillastar.vsbrewing.utils.getModIdentifier
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.render.entity.model.EntityModelLayer
import net.minecraft.component.DataComponentTypes
import net.minecraft.util.math.Direction

val FLASK_CONTENT_MODEL_LAYERS =
    (FlaskBlock.LEVEL.values).associate {
      it to EntityModelLayer(getModIdentifier("flask_level${it}"), "root")
    }

/** [BlockEntityRenderer] for the content within a placed flask. */
@Environment(EnvType.CLIENT)
class FlaskBlockEntityRenderer(context: BlockEntityRendererFactory.Context) :
    LeveledBlockEntityRenderer<FlaskBlockEntity>(context) {
  companion object {
    fun getContentTexturedModelData(level: Int) =
        getContentTexturedModelData(
            level,
            FlaskBlock.LEVEL,
            setOf(
                Direction.DOWN,
                Direction.UP,
                Direction.NORTH,
                Direction.SOUTH,
                Direction.WEST,
                Direction.EAST,
            ),
            sizeXZ = 10.0f,
            sizeY =
                when (level) {
                  1 -> 5.0f
                  2 -> 9.0f
                  3 -> 13.0f
                  else -> 0.0f
                },
            offsetXZ = 3.0f,
            offsetY = 1.0f,
        )
  }

  override fun getLeveledEntityModelLayerMap() = FLASK_CONTENT_MODEL_LAYERS

  override fun getLevel(entity: FlaskBlockEntity) =
      entity.world?.getBlockState(entity.pos)?.getOrEmpty(FlaskBlock.LEVEL)?.orElse(0) ?: 0

  override fun getSpriteId(entity: FlaskBlockEntity) =
      when {
        entity.item.isOf(MOD_ITEMS.potionFlaskItem) -> {
          val potionContents = entity.item.get(DataComponentTypes.POTION_CONTENTS)
          when {
            potionContentsMatchId(potionContents, MILK_POTION_ID) -> MILK_SPRITE_ID
            else -> WATER_SPRITE_ID
          }
        }
        else -> null
      }

  override fun getColor(entity: FlaskBlockEntity) = entity.renderData
}
