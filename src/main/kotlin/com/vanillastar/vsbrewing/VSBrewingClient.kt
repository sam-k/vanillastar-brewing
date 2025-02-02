package com.vanillastar.vsbrewing

import com.vanillastar.vsbrewing.item.MOD_COLOR_PROVIDERS
import com.vanillastar.vsbrewing.item.MOD_MODEL_PREDICATE_PROVIDERS
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

@Environment(EnvType.CLIENT)
object VSBrewingClient : ClientModInitializer {
  override fun onInitializeClient() {
    MOD_MODEL_PREDICATE_PROVIDERS.initialize()
    MOD_COLOR_PROVIDERS.initialize()
  }
}
