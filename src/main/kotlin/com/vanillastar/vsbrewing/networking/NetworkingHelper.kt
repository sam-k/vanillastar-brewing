package com.vanillastar.vsbrewing.networking

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload

typealias ModNetworkingClientCallback<TPayload> =
    (TPayload, ClientPlayNetworking.Context) -> () -> Unit

sealed interface ModNetworkingPayload : CustomPayload {
  interface Companion<TPayload : ModNetworkingPayload> {
    val id: CustomPayload.Id<TPayload>
    val codec: PacketCodec<RegistryByteBuf, TPayload>
  }

  interface ServerCompanion<TPayload : ModNetworkingPayload> : Companion<TPayload> {
    val callback: ModNetworkingClientCallback<TPayload>
  }

  interface Server<TPayload : ModNetworkingPayload> : ModNetworkingPayload {
    val companion: ServerCompanion<TPayload>
  }
}
