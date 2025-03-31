package com.vanillastar.vsbrewing.color

import com.vanillastar.vsbrewing.block.MOD_BLOCKS
import com.vanillastar.vsbrewing.block.entity.AbstractPotionCauldronBlockEntity
import com.vanillastar.vsbrewing.block.entity.BottleBlockEntity
import com.vanillastar.vsbrewing.block.entity.BrewingStandBlockEntityRenderData
import com.vanillastar.vsbrewing.item.MOD_ITEMS
import com.vanillastar.vsbrewing.utils.ModRegistry
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.client.color.block.BlockColorProvider
import net.minecraft.client.color.item.ItemColorProvider
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.util.math.ColorHelper.Argb

@Environment(EnvType.CLIENT)
abstract class ModColorProviders : ModRegistry() {
  override fun initialize() {
    this.registerItemColorProvider(
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

    this.registerBlockColorProvider(
        { stack, world, pos, tintIndex ->
          if (world != null && pos != null) {
            val renderData = world.getBlockEntityRenderData(pos)
            if (
                renderData is BrewingStandBlockEntityRenderData &&
                    tintIndex >= 0 &&
                    tintIndex < renderData.colors.length()
            ) {
              Argb.fullAlpha(renderData.colors[tintIndex])
            } else -1
          } else -1
        },
        Blocks.BREWING_STAND,
    )

    this.registerBlockColorProvider(
        { stack, world, pos, tintIndex ->
          if (tintIndex == 0 && world != null && pos != null) {
            val renderData = world.getBlockEntityRenderData(pos)
            if (renderData is AbstractPotionCauldronBlockEntity.RenderData) {
              Argb.fullAlpha(renderData.color)
            } else -1
          } else -1
        },
        MOD_BLOCKS.potionCauldronBlock,
        MOD_BLOCKS.potionCauldronPreviewBlock,
    )

    this.registerBlockColorProvider(
        { stack, world, pos, tintIndex ->
          if (tintIndex == 0 && world != null && pos != null) {
            val renderData = world.getBlockEntityRenderData(pos)
            if (renderData is Int) {
              Argb.fullAlpha(renderData)
            } else -1
          } else -1
        },
        MOD_BLOCKS.flaskBlock,
    )

    // Coloring the bottles themselves should also belong to BottleBlockEntityRenderer, which would
    // require dynamically rendering the bottles as entities. (Doing so would also reduce code
    // duplication in the BottleBlock JSON models.) But culling obscured faces is far easier with
    // traditional baked block models.
    this.registerBlockColorProvider(
        { stack, world, pos, tintIndex ->
          if (world == null || pos == null) {
            return@registerBlockColorProvider -1
          }
          val renderData = world.getBlockEntityRenderData(pos)
          if (renderData !is BottleBlockEntity.RenderData) {
            return@registerBlockColorProvider -1
          }
          // Bottle block tint indices alternate between body colors and cork colors.
          val colors = if (tintIndex % 2 == 0) renderData.bodyColors else renderData.corkColors
          val normalizedTintIndex = tintIndex / 2
          if (normalizedTintIndex >= colors.length()) {
            return@registerBlockColorProvider -1
          }
          Argb.fullAlpha(colors[normalizedTintIndex])
        },
        MOD_BLOCKS.bottleBlock,
    )
  }

  private fun registerItemColorProvider(provider: ItemColorProvider, vararg items: Item) {
    ColorProviderRegistry.ITEM.register(provider, *items)
    this.logger.info("Registered item color provider for items {}", items.joinToString(", "))
  }

  private fun registerBlockColorProvider(provider: BlockColorProvider, vararg blocks: Block) {
    ColorProviderRegistry.BLOCK.register(provider, *blocks)
    this.logger.info(
        "Registered block color provider for blocks {}",
        blocks.joinToString(", ") { Registries.BLOCK.getEntry(it).idAsString },
    )
  }
}

@JvmField val MOD_COLOR_PROVIDERS = object : ModColorProviders() {}
