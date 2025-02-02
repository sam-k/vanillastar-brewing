package com.vanillastar.vsbrewing

import com.vanillastar.vsbrewing.component.MOD_COMPONENTS
import com.vanillastar.vsbrewing.item.MOD_ITEMS
import com.vanillastar.vsbrewing.recipe.MOD_BREWING_RECIPES
import net.fabricmc.api.ModInitializer

const val MOD_ID = "vsbrewing"

object VSBrewing : ModInitializer {
  override fun onInitialize() {
    MOD_COMPONENTS.initialize()
    MOD_ITEMS.initialize()
    MOD_BREWING_RECIPES.initialize()
  }
}
