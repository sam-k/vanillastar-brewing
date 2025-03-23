package com.vanillastar.vsbrewing

import com.vanillastar.vsbrewing.block.MOD_BLOCKS
import com.vanillastar.vsbrewing.block.MOD_CAULDRON_BEHAVIORS
import com.vanillastar.vsbrewing.block.entity.MOD_BLOCK_ENTITIES
import com.vanillastar.vsbrewing.component.MOD_COMPONENTS
import com.vanillastar.vsbrewing.item.MOD_ITEMS
import com.vanillastar.vsbrewing.recipe.MOD_BREWING_RECIPES
import com.vanillastar.vsbrewing.screen.MOD_SCREEN_HANDLERS
import com.vanillastar.vsbrewing.tag.MOD_TAGS
import net.fabricmc.api.ModInitializer

const val MOD_ID = "vsbrewing"

object VSBrewing : ModInitializer {
  override fun onInitialize() {
    MOD_TAGS.initialize()
    MOD_BLOCKS.initialize()
    MOD_BLOCK_ENTITIES.initialize()
    MOD_COMPONENTS.initialize()
    MOD_ITEMS.initialize()
    MOD_BREWING_RECIPES.initialize()
    MOD_CAULDRON_BEHAVIORS.initialize()
    MOD_SCREEN_HANDLERS.initialize()
  }
}
