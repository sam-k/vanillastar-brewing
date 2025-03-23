package com.vanillastar.vsbrewing.render

import com.vanillastar.vsbrewing.block.BottleBlock
import com.vanillastar.vsbrewing.block.FlaskBlock
import com.vanillastar.vsbrewing.utils.ModRegistry
import com.vanillastar.vsbrewing.utils.getModIdentifier
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry
import net.minecraft.client.render.entity.model.EntityModelLayer

@Environment(EnvType.CLIENT)
abstract class ModEntityModelLayers : ModRegistry() {
  companion object {
    val BOTTLE_CONTENT_MODEL_LAYERS =
        (BottleBlock.COUNT.values).associate {
          it to EntityModelLayer(getModIdentifier("bottle_count${it}"), "root")
        }

    val FLASK_CONTENT_MODEL_LAYERS =
        (FlaskBlock.LEVEL.values).associate {
          it to EntityModelLayer(getModIdentifier("flask_level${it}"), "root")
        }
  }

  override fun initialize() {
    for ((count, modelLayer) in BOTTLE_CONTENT_MODEL_LAYERS) {
      this.registerModelLayer(modelLayer) {
        BottleBlockEntityRenderer.getContentTexturedModelData(count)
      }
    }
    for ((level, modelLayer) in FLASK_CONTENT_MODEL_LAYERS) {
      this.registerModelLayer(modelLayer) {
        FlaskBlockEntityRenderer.getContentTexturedModelData(level)
      }
    }
  }

  private fun registerModelLayer(
      modelLayer: EntityModelLayer,
      modelDataProvider: EntityModelLayerRegistry.TexturedModelDataProvider,
  ) {
    EntityModelLayerRegistry.registerModelLayer(modelLayer, modelDataProvider)
    this.logger.info("Registered model layer {}", modelLayer)
  }
}

@JvmField val MOD_ENTITY_MODEL_LAYERS = object : ModEntityModelLayers() {}
