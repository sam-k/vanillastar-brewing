package com.vanillastar.vsbrewing.render

import com.vanillastar.vsbrewing.block.FlaskBlock
import com.vanillastar.vsbrewing.block.entity.FlaskBlockEntity
import com.vanillastar.vsbrewing.render.ModEntityModelLayers.Companion.FLASK_CONTENT_MODEL_LAYERS
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.model.ModelData
import net.minecraft.client.model.TexturedModelData
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.util.SpriteIdentifier
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.screen.PlayerScreenHandler
import net.minecraft.util.Identifier
import net.minecraft.util.math.Direction

/** [BlockEntityRenderer] for the content within a placed flask. */
@Environment(EnvType.CLIENT)
class FlaskBlockEntityRenderer(context: BlockEntityRendererFactory.Context) :
    BlockEntityRenderer<FlaskBlockEntity> {
  companion object {
    private const val TEXTURE_DIM = 16

    private val WATER_SPRITE_ID =
        SpriteIdentifier(
            PlayerScreenHandler.BLOCK_ATLAS_TEXTURE,
            Identifier.ofVanilla("block/water_still"),
        )

    fun getContentTexturedModelData(level: Int): TexturedModelData {
      if (level < 1 || level !in FlaskBlock.LEVEL.values) {
        return TexturedModelData.of(ModelData(), TEXTURE_DIM, TEXTURE_DIM)
      }

      val modelData = ModelData()
      val root = modelData.root

      val sizeXZ = 10.0f
      val sizeY =
          when (level) {
            1 -> 5.0f
            2 -> 9.0f
            3 -> 13.0f
            else -> 0.0f
          }
      val offsetXZ = 3.0f
      val offsetY = 1.0f
      fun getContentBuilder(dir: Direction) =
          getModelPartBuilder(
              dir,
              sizeXZ,
              sizeY,
              sizeXZ,
              mapOf(
                  Direction.DOWN to (offsetXZ - sizeXZ).toInt(),
                  Direction.NORTH to offsetXZ.toInt(),
                  Direction.WEST to offsetXZ.toInt(),
              ),
              mapOf(
                  Direction.DOWN to offsetXZ.toInt(),
                  Direction.NORTH to (TEXTURE_DIM - offsetY - sizeY).toInt(),
                  Direction.WEST to (TEXTURE_DIM - offsetY - sizeXZ - sizeY).toInt(),
              ),
          )
      fun getContentTransform(dir: Direction) =
          getModelTransform(
              dir,
              offsetXZ + 0.5f * sizeXZ,
              offsetY + 0.5f * sizeY,
              offsetXZ + 0.5f * sizeXZ,
          )

      val downBuilder = getContentBuilder(Direction.DOWN)
      root.addChild("content_down", downBuilder, getContentTransform(Direction.DOWN))
      root.addChild("content_up", downBuilder, getContentTransform(Direction.UP))

      val northBuilder = getContentBuilder(Direction.NORTH)
      root.addChild("content_north", northBuilder, getContentTransform(Direction.NORTH))
      root.addChild("content_south", northBuilder, getContentTransform(Direction.SOUTH))

      val westBuilder = getContentBuilder(Direction.WEST)
      root.addChild("content_west", westBuilder, getContentTransform(Direction.WEST))
      root.addChild("content_east", westBuilder, getContentTransform(Direction.EAST))

      return TexturedModelData.of(modelData, TEXTURE_DIM, TEXTURE_DIM)
    }
  }

  val contentByLevel =
      FLASK_CONTENT_MODEL_LAYERS.mapValues { (_, modelLayer) ->
        context.getLayerModelPart(modelLayer)
      }

  override fun render(
      entity: FlaskBlockEntity,
      tickDelta: Float,
      matrices: MatrixStack,
      vertices: VertexConsumerProvider,
      light: Int,
      overlay: Int,
  ) {
    val level =
        entity.world?.getBlockState(entity.pos)?.getOrEmpty(FlaskBlock.LEVEL)?.orElse(0) ?: 0
    if (level <= 0) {
      return
    }

    matrices.push()
    this.contentByLevel[level]?.render(
        matrices,
        WATER_SPRITE_ID.getVertexConsumer(vertices) { RenderLayer.getEntityTranslucent(it) },
        light,
        overlay,
        entity.renderData,
    )
    matrices.pop()
  }
}
