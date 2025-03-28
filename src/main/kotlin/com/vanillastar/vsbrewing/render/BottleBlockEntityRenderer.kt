package com.vanillastar.vsbrewing.render

import com.vanillastar.vsbrewing.block.BottleBlock
import com.vanillastar.vsbrewing.block.entity.BottleBlockEntity
import com.vanillastar.vsbrewing.potion.MILK_POTION_ID
import com.vanillastar.vsbrewing.potion.potionContentsMatchId
import com.vanillastar.vsbrewing.utils.getModIdentifier
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.model.ModelData
import net.minecraft.client.model.ModelPartBuilder
import net.minecraft.client.model.ModelPartData
import net.minecraft.client.model.ModelTransform
import net.minecraft.client.model.TexturedModelData
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.render.entity.model.EntityModelLayer
import net.minecraft.client.util.SpriteIdentifier
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.component.DataComponentTypes
import net.minecraft.item.Items
import net.minecraft.screen.PlayerScreenHandler
import net.minecraft.util.Identifier
import net.minecraft.util.math.Direction

val BOTTLE_CONTENT_MODEL_LAYERS =
    (BottleBlock.COUNT.values).associate {
      it to EntityModelLayer(getModIdentifier("bottle_count${it}"), "root")
    }

/** [BlockEntityRenderer] for the contents within placed bottles. */
@Environment(EnvType.CLIENT)
class BottleBlockEntityRenderer(context: BlockEntityRendererFactory.Context) :
    BlockEntityRenderer<BottleBlockEntity> {
  companion object {
    private const val TEXTURE_DIM = 16

    private val DRAGON_BREATH_SPRITE_ID =
        SpriteIdentifier(
            PlayerScreenHandler.BLOCK_ATLAS_TEXTURE,
            getModIdentifier("block/dragon_breath"),
        )
    private val EXPERIENCE_SPRITE_ID =
        SpriteIdentifier(
            PlayerScreenHandler.BLOCK_ATLAS_TEXTURE,
            getModIdentifier("block/experience"),
        )
    private val HONEY_SPRITE_ID =
        SpriteIdentifier(
            PlayerScreenHandler.BLOCK_ATLAS_TEXTURE,
            Identifier.ofVanilla("block/honey_block_side"),
        )
    private val MILK_SPRITE_ID =
        SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, getModIdentifier("block/milk"))
    private val OMINOUS_SPRITE_ID =
        SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, getModIdentifier("block/ominous"))
    private val WATER_SPRITE_ID =
        SpriteIdentifier(
            PlayerScreenHandler.BLOCK_ATLAS_TEXTURE,
            Identifier.ofVanilla("block/water_still"),
        )

    private fun getChildRootName(index: Int) = "root${index}"

    private fun addContentModelPartData(
        root: ModelPartData,
        childName: String,
        offsetX: Float,
        offsetZ: Float,
    ) {
      val size = 3.0f
      val offsetY = 1.0f

      fun getBodyBuilder(dir: Direction) =
          getModelPartBuilder(
              dir,
              size,
              uMap =
                  mapOf(
                      Direction.DOWN to size.toInt(),
                      Direction.NORTH to (2 * size).toInt(),
                      Direction.WEST to (2 * size).toInt(),
                  ),
              vMap =
                  mapOf(
                      Direction.DOWN to (2 * size).toInt(),
                      Direction.NORTH to (2 * size).toInt(),
                      Direction.WEST to size.toInt(),
                  ),
          )

      fun getBodyTransform(dir: Direction) =
          getModelTransform(
              dir,
              offsetX + 0.5f * size,
              offsetY + 0.5f * size,
              offsetZ + 0.5f * size,
          )

      val child = root.addChild(childName, ModelPartBuilder.create(), ModelTransform.NONE)

      val downBodyBuilder = getBodyBuilder(Direction.DOWN)
      child.addChild("body_down", downBodyBuilder, getBodyTransform(Direction.DOWN))
      child.addChild("body_up", downBodyBuilder, getBodyTransform(Direction.UP))

      val northBodyBuilder = getBodyBuilder(Direction.NORTH)
      child.addChild("body_north", northBodyBuilder, getBodyTransform(Direction.NORTH))
      child.addChild("body_south", northBodyBuilder, getBodyTransform(Direction.SOUTH))

      val westBodyBuilder = getBodyBuilder(Direction.WEST)
      child.addChild("body_west", westBodyBuilder, getBodyTransform(Direction.WEST))
      child.addChild("body_east", westBodyBuilder, getBodyTransform(Direction.EAST))
    }

    fun getContentTexturedModelData(count: Int): TexturedModelData {
      if (count !in BottleBlock.COUNT.values) {
        return TexturedModelData.of(ModelData(), TEXTURE_DIM, TEXTURE_DIM)
      }

      val modelData = ModelData()
      val root = modelData.root
      when (count) {
        1 -> {
          addContentModelPartData(root, getChildRootName(1), offsetX = 6.0f, offsetZ = 6.0f)
        }
        2 -> {
          addContentModelPartData(root, getChildRootName(1), offsetX = 3.0f, offsetZ = 9.0f)
          addContentModelPartData(root, getChildRootName(2), offsetX = 9.0f, offsetZ = 3.0f)
        }
        3 -> {
          addContentModelPartData(root, getChildRootName(1), offsetX = 3.0f, offsetZ = 3.0f)
          addContentModelPartData(root, getChildRootName(2), offsetX = 10.0f, offsetZ = 4.0f)
          addContentModelPartData(root, getChildRootName(3), offsetX = 6.0f, offsetZ = 10.0f)
        }
        4 -> {
          addContentModelPartData(root, getChildRootName(1), offsetX = 3.0f, offsetZ = 4.0f)
          addContentModelPartData(root, getChildRootName(2), offsetX = 9.0f, offsetZ = 3.0f)
          addContentModelPartData(root, getChildRootName(3), offsetX = 4.0f, offsetZ = 10.0f)
          addContentModelPartData(root, getChildRootName(4), offsetX = 10.0f, offsetZ = 9.0f)
        }
      }

      return TexturedModelData.of(modelData, TEXTURE_DIM, TEXTURE_DIM)
    }
  }

  val bottlesByCount =
      BOTTLE_CONTENT_MODEL_LAYERS.mapValues { (_, modelLayer) ->
        context.getLayerModelPart(modelLayer)
      }

  override fun render(
      entity: BottleBlockEntity,
      tickDelta: Float,
      matrices: MatrixStack,
      vertices: VertexConsumerProvider,
      light: Int,
      overlay: Int,
  ) {
    val count =
        entity.world?.getBlockState(entity.pos)?.getOrEmpty(BottleBlock.COUNT)?.orElse(0) ?: 0
    if (count <= 0) {
      return
    }

    matrices.push()
    for ((i, stack) in entity.iterateItems().withIndex()) {
      val spriteId =
          when {
            stack.isOf(Items.DRAGON_BREATH) -> DRAGON_BREATH_SPRITE_ID
            stack.isOf(Items.EXPERIENCE_BOTTLE) -> EXPERIENCE_SPRITE_ID
            stack.isOf(Items.HONEY_BOTTLE) -> HONEY_SPRITE_ID
            stack.isOf(Items.OMINOUS_BOTTLE) -> OMINOUS_SPRITE_ID
            stack.isOf(Items.POTION) -> {
              when {
                potionContentsMatchId(
                    stack.get(DataComponentTypes.POTION_CONTENTS),
                    MILK_POTION_ID,
                ) -> MILK_SPRITE_ID
                else -> WATER_SPRITE_ID
              }
            }
            else -> null
          }
      if (spriteId == null) {
        continue
      }

      this.bottlesByCount[count]
          ?.getChild(getChildRootName(i + 1))
          ?.render(
              matrices,
              spriteId.getVertexConsumer(vertices) { RenderLayer.getEntityTranslucent(it) },
              light,
              overlay,
              when {
                potionContentsMatchId(
                    stack.get(DataComponentTypes.POTION_CONTENTS),
                    MILK_POTION_ID,
                ) -> -1
                else -> stack.get(DataComponentTypes.POTION_CONTENTS)?.color ?: -1
              },
          )
    }
    matrices.pop()
  }
}
