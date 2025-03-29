package com.vanillastar.vsbrewing.render

import com.vanillastar.vsbrewing.block.entity.MOD_BLOCK_ENTITIES
import com.vanillastar.vsbrewing.utils.ModRegistry
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory

@Environment(EnvType.CLIENT)
abstract class ModBlockEntityRenderers : ModRegistry() {
  override fun initialize() {
    this.registerBlockEntityRenderer(
        MOD_BLOCK_ENTITIES.potionCauldronBlockEntityType,
        ::PotionCauldronBlockEntityRenderer,
    )
    this.registerBlockEntityRenderer(
        MOD_BLOCK_ENTITIES.potionCauldronPreviewBlockEntityType,
        ::PotionCauldronPreviewBlockEntityRenderer,
    )
    this.registerBlockEntityRenderer(
        MOD_BLOCK_ENTITIES.bottleBlockEntityType,
        ::BottleBlockEntityRenderer,
    )
    this.registerBlockEntityRenderer(
        MOD_BLOCK_ENTITIES.flaskBlockEntityType,
        ::FlaskBlockEntityRenderer,
    )
  }

  private fun <
      TBlockEntity : BlockEntity,
      TBlockEntityRenderer : BlockEntityRenderer<TBlockEntity>,
  > registerBlockEntityRenderer(
      blockEntityType: BlockEntityType<TBlockEntity>,
      constructor: (BlockEntityRendererFactory.Context) -> TBlockEntityRenderer,
  ) {
    BlockEntityRendererFactories.register(blockEntityType, constructor)
    this.logger.info(
        "Registered block entity renderer for block entity {}",
        blockEntityType.registryEntry?.idAsString,
    )
  }
}

@JvmField val MOD_BLOCK_ENTITY_RENDERERS = object : ModBlockEntityRenderers() {}
