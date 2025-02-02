package com.vanillastar.vsbrewing.item

import com.vanillastar.vsbrewing.utils.ModRegistry
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry
import net.minecraft.client.color.item.ItemColorProvider
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.math.ColorHelper

abstract class ModColorProviders : ModRegistry() {
  override fun initialize() {
    registerItemColorProvider(
        { stack: ItemStack, tintIndex: Int ->
          if (tintIndex > 0) -1
          else
              ColorHelper.Argb.fullAlpha(
                  stack
                      .getOrDefault(
                          DataComponentTypes.POTION_CONTENTS,
                          PotionContentsComponent.DEFAULT,
                      )
                      .color
              )
        },
        MOD_ITEMS.potionFlaskItem,
    )
  }

  private fun registerItemColorProvider(provider: ItemColorProvider, vararg items: Item) {
    ColorProviderRegistry.ITEM.register(provider, *items)
    logger.info("Registered item color provider for items {}", items.joinToString(", "))
  }
}

val MOD_COLOR_PROVIDERS = object : ModColorProviders() {}
