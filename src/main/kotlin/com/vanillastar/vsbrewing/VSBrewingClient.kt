package com.vanillastar.vsbrewing

import com.vanillastar.vsbrewing.color.MOD_COLOR_PROVIDERS
import com.vanillastar.vsbrewing.gui.MOD_HANDLED_SCREENS
import com.vanillastar.vsbrewing.item.MOD_MODEL_PREDICATE_PROVIDERS
import com.vanillastar.vsbrewing.networking.MOD_CLIENT_NETWORKING
import com.vanillastar.vsbrewing.particle.MOD_PARTICLE_FACTORIES
import com.vanillastar.vsbrewing.render.MOD_BLOCK_ENTITY_RENDERERS
import com.vanillastar.vsbrewing.render.MOD_ENTITY_MODEL_LAYERS
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

@Environment(EnvType.CLIENT)
object VSBrewingClient : ClientModInitializer {
  override fun onInitializeClient() {
    MOD_MODEL_PREDICATE_PROVIDERS.initialize()
    MOD_BLOCK_ENTITY_RENDERERS.initialize()
    MOD_ENTITY_MODEL_LAYERS.initialize()
    MOD_COLOR_PROVIDERS.initialize()
    MOD_PARTICLE_FACTORIES.initialize()
    MOD_HANDLED_SCREENS.initialize()
    MOD_CLIENT_NETWORKING.initialize()
  }
}
