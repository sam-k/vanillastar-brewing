package com.vanillastar.vsbrewing;

import static com.vanillastar.vsbrewing.block.ModBlocksKt.MOD_BLOCKS;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;

public class VSBrewingMixinClient implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    // This counts as a mixin and therefore must be done in Java.
    BlockRenderLayerMap.INSTANCE.putBlocks(
        RenderLayer.getCutout(), MOD_BLOCKS.potionCauldronPreviewBlock);
    BlockRenderLayerMap.INSTANCE.putBlocks(
        RenderLayer.getCutoutMipped(), MOD_BLOCKS.bottleBlock, MOD_BLOCKS.flaskBlock);
  }
}
