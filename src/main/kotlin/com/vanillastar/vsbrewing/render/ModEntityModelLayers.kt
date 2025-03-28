package com.vanillastar.vsbrewing.render

import com.vanillastar.vsbrewing.utils.ModRegistry
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry
import net.minecraft.client.render.entity.model.EntityModelLayer

@Environment(EnvType.CLIENT)
abstract class ModEntityModelLayers : ModRegistry() {
  override fun initialize() {
    for ((level, modelLayer) in POTION_CAULDRON_CONTENT_MODEL_LAYERS) {
      this.registerModelLayer(modelLayer) {
        PotionCauldronBlockEntityRenderer.getContentTexturedModelData(level)
      }
    }
    for ((level, modelLayer) in POTION_CAULDRON_PREVIEW_CONTENT_MODEL_LAYERS) {
      this.registerModelLayer(modelLayer) {
        PotionCauldronPreviewBlockEntityRenderer.getContentTexturedModelData(level)
      }
    }
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
