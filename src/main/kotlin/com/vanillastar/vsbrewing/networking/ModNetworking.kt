package com.vanillastar.vsbrewing.networking

import com.vanillastar.vsbrewing.utils.ModRegistry
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry

abstract class ModNetworking : ModRegistry() {
  private val serverPayloads = listOf(BrewingCauldronPayload)

  private fun <TPayload : ModNetworkingPayload> registerServerPayload(
      payloadCompanion: ModNetworkingPayload.ServerCompanion<TPayload>
  ) {
    PayloadTypeRegistry.playS2C().register(payloadCompanion.id, payloadCompanion.codec)
    logger.info("Registered server payload {}", payloadCompanion.id.id)
  }

  private fun <TPayload : ModNetworkingPayload> registerClientReceiver(
      payloadCompanion: ModNetworkingPayload.ServerCompanion<TPayload>
  ) {
    ClientPlayNetworking.registerGlobalReceiver(payloadCompanion.id) { payload, context ->
      context.client().execute(payloadCompanion.callback(payload, context))
    }
    logger.info("Registered client receiver for server payload {}", payloadCompanion.id.id)
  }

  override fun initialize() {
    serverPayloads.forEach {
      registerServerPayload(it)
      registerClientReceiver(it)
    }
  }
}

@JvmField val MOD_NETWORKING = object : ModNetworking() {}
