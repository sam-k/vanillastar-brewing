package com.vanillastar.vsbrewing;

import static com.vanillastar.vsbrewing.block.ModBlocksKt.MOD_BLOCKS;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;

public class VSBrewingMixinClient implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    // This counts as a mixin and therefore must be done in Java.
    BlockRenderLayerMap.INSTANCE.putBlock(
        MOD_BLOCKS.brewingCauldronStandBlock, RenderLayer.getCutout());
  }
}
