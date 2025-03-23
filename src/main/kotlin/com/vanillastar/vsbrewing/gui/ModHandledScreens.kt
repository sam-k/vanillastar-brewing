package com.vanillastar.vsbrewing.gui

import com.vanillastar.vsbrewing.screen.MOD_SCREEN_HANDLERS
import com.vanillastar.vsbrewing.utils.ModRegistry
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.screen.ingame.HandledScreens
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerType

@Environment(EnvType.CLIENT)
abstract class ModHandledScreens : ModRegistry() {
  override fun initialize() {
    this.registerScreen(MOD_SCREEN_HANDLERS.brewingCauldronScreenHandler, ::BrewingCauldronScreen)
  }

  private fun <
      TScreenHandler : ScreenHandler,
      TScreen : HandledScreen<TScreenHandler>,
  > registerScreen(
      screenHandler: ScreenHandlerType<TScreenHandler>,
      constructor: HandledScreens.Provider<TScreenHandler, TScreen>,
  ) {
    HandledScreens.register(screenHandler, constructor)
    this.logger.info("Registered screen handler {}", screenHandler)
  }
}

@JvmField val MOD_HANDLED_SCREENS = object : ModHandledScreens() {}
