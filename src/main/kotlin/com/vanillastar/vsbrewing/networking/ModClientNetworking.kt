package com.vanillastar.vsbrewing.networking

import com.vanillastar.vsbrewing.utils.ModRegistry
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry

@Environment(EnvType.CLIENT)
abstract class ModClientNetworking : ModRegistry() {
  private val serverPayloads = listOf(BrewingCauldronPayload, ThrownPotionPayload)

  private fun <TPayload : ModNetworkingPayload> registerServerPayload(
      payloadCompanion: ModNetworkingPayload.ServerCompanion<TPayload>
  ) {
    PayloadTypeRegistry.playS2C().register(payloadCompanion.id, payloadCompanion.codec)
    this.logger.info("Registered server payload {}", payloadCompanion.id.id)
  }

  private fun <TPayload : ModNetworkingPayload> registerClientReceiver(
      payloadCompanion: ModNetworkingPayload.ServerCompanion<TPayload>
  ) {
    ClientPlayNetworking.registerGlobalReceiver(payloadCompanion.id) { payload, context ->
      context.client().execute(payloadCompanion.callback(payload, context))
    }
    this.logger.info("Registered client receiver for server payload {}", payloadCompanion.id.id)
  }

  override fun initialize() {
    serverPayloads.forEach {
      this.registerServerPayload(it)
      this.registerClientReceiver(it)
    }
  }
}

@JvmField val MOD_CLIENT_NETWORKING = object : ModClientNetworking() {}
