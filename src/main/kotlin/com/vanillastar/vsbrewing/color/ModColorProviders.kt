package com.vanillastar.vsbrewing.color

import com.vanillastar.vsbrewing.block.MOD_BLOCKS
import com.vanillastar.vsbrewing.item.MOD_ITEMS
import com.vanillastar.vsbrewing.utils.ModRegistry
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry
import net.minecraft.block.Block
import net.minecraft.client.color.block.BlockColorProvider
import net.minecraft.client.color.item.ItemColorProvider
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.item.Item
import net.minecraft.util.math.ColorHelper.Argb

@Environment(EnvType.CLIENT)
abstract class ModColorProviders : ModRegistry() {
  override fun initialize() {
    registerItemColorProvider(
        { stack, tintIndex ->
          if (tintIndex == 0) {
            Argb.fullAlpha(
                stack
                    .getOrDefault(
                        DataComponentTypes.POTION_CONTENTS,
                        PotionContentsComponent.DEFAULT,
                    )
                    .color
            )
          } else -1
        },
        MOD_ITEMS.potionFlaskItem,
    )

    registerBlockColorProvider(
        { stack, world, pos, tintIndex ->
          if (tintIndex == 0 && world != null && pos != null) {
            val renderData = world.getBlockEntityRenderData(pos)
            if (renderData is Int) Argb.fullAlpha(renderData) else -1
          } else -1
        },
        MOD_BLOCKS.potionCauldronBlock,
        MOD_BLOCKS.potionCauldronPreviewBlock,
        MOD_BLOCKS.flaskBlock,
    )
  }

  private fun registerItemColorProvider(provider: ItemColorProvider, vararg items: Item) {
    ColorProviderRegistry.ITEM.register(provider, *items)
    logger.info("Registered item color provider for items {}", items.joinToString(", "))
  }

  private fun registerBlockColorProvider(provider: BlockColorProvider, vararg blocks: Block) {
    ColorProviderRegistry.BLOCK.register(provider, *blocks)
    logger.info("Registered block color provider for blocks {}", blocks.joinToString(", "))
  }
}

@JvmField val MOD_COLOR_PROVIDERS = object : ModColorProviders() {}
