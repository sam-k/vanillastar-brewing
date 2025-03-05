package com.vanillastar.vsbrewing.gui

import com.vanillastar.vsbrewing.screen.MOD_SCREEN_HANDLERS
import com.vanillastar.vsbrewing.utils.ModRegistry
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.screen.ingame.HandledScreens

@Environment(EnvType.CLIENT)
abstract class ModHandledScreens : ModRegistry() {
  override fun initialize() {
    HandledScreens.register(
        MOD_SCREEN_HANDLERS.brewingCauldronScreenHandler,
        ::BrewingCauldronScreen,
    )
  }
}

@JvmField val MOD_HANDLED_SCREENS = object : ModHandledScreens() {}
