package com.vanillastar.vsbrewing.render

import com.vanillastar.vsbrewing.utils.getModIdentifier
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.model.ModelData
import net.minecraft.client.model.TexturedModelData
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.render.entity.model.EntityModelLayer
import net.minecraft.client.util.SpriteIdentifier
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.screen.PlayerScreenHandler
import net.minecraft.state.property.IntProperty
import net.minecraft.util.Identifier
import net.minecraft.util.math.Direction

@Environment(EnvType.CLIENT)
abstract class LeveledBlockEntityRenderer<TBlockEntity : BlockEntity>(
    context: BlockEntityRendererFactory.Context
) : BlockEntityRenderer<TBlockEntity> {
  companion object {
    private const val TEXTURE_DIM = 16

    val MILK_SPRITE_ID =
        SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, getModIdentifier("block/milk"))
    val WATER_SPRITE_ID =
        SpriteIdentifier(
            PlayerScreenHandler.BLOCK_ATLAS_TEXTURE,
            Identifier.ofVanilla("block/water_still"),
        )

    fun getContentTexturedModelData(
        level: Int,
        levelProperty: IntProperty,
        renderedDirs: Set<Direction>,
        sizeXZ: Float,
        sizeY: Float,
        offsetXZ: Float,
        offsetY: Float,
    ): TexturedModelData {
      if (level <= 0 || level !in levelProperty.values) {
        return TexturedModelData.of(ModelData(), TEXTURE_DIM, TEXTURE_DIM)
      }

      val modelData = ModelData()
      val root = modelData.root

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

      for (dir in renderedDirs) {
        root.addChild("content_${dir.name}", getContentBuilder(dir), getContentTransform(dir))
      }

      return TexturedModelData.of(modelData, TEXTURE_DIM, TEXTURE_DIM)
    }
  }

  abstract fun getLeveledEntityModelLayerMap(): Map<Int, EntityModelLayer>

  abstract fun getLevel(entity: TBlockEntity): Int

  abstract fun getSpriteId(entity: TBlockEntity): SpriteIdentifier?

  abstract fun getColor(entity: TBlockEntity): Int

  private val contentByLevel =
      this.getLeveledEntityModelLayerMap().mapValues { (_, modelLayer) ->
        context.getLayerModelPart(modelLayer)
      }

  override fun render(
      entity: TBlockEntity,
      tickDelta: Float,
      matrices: MatrixStack,
      vertices: VertexConsumerProvider,
      light: Int,
      overlay: Int,
  ) {
    val level = this.getLevel(entity)
    if (level <= 0) {
      return
    }

    val spriteId = this.getSpriteId(entity)
    if (spriteId == null) {
      return
    }

    matrices.push()
    this.contentByLevel[level]?.render(
        matrices,
        spriteId.getVertexConsumer(vertices) { RenderLayer.getEntityTranslucent(it) },
        light,
        overlay,
        this.getColor(entity),
    )
    matrices.pop()
  }
}
