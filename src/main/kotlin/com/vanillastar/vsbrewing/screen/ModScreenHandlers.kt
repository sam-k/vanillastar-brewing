package com.vanillastar.vsbrewing.screen

import com.vanillastar.vsbrewing.utils.ModRegistry
import com.vanillastar.vsbrewing.utils.getModIdentifier
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.resource.featuretoggle.FeatureSet
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerType

abstract class ModScreenHandlers : ModRegistry() {
  @JvmField
  val brewingCauldronScreenHandler =
      this.registerScreenHandler(
          BREWING_CAULDRON_SCREEN_HANDLER_NAME,
          ::BrewingCauldronScreenHandler,
      )

  @Suppress("SameParameterValue")
  private fun <TScreenHandler : ScreenHandler> registerScreenHandler(
      name: String,
      factory: ScreenHandlerType.Factory<TScreenHandler>,
  ): ScreenHandlerType<TScreenHandler> {
    val screenHandlerType =
        Registry.register(
            Registries.SCREEN_HANDLER,
            getModIdentifier(name),
            ScreenHandlerType<TScreenHandler>(factory, FeatureSet.empty()),
        )
    this.logger.info("Registered screen handler {}", name)
    return screenHandlerType
  }

  override fun initialize() {}
}

@JvmField val MOD_SCREEN_HANDLERS = object : ModScreenHandlers() {}
