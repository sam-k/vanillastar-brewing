package com.vanillastar.vsbrewing.networking

import kotlin.math.pow
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.SharedConstants
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

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

fun sendWorldEvent(world: World, pos: BlockPos, payload: ModNetworkingPayload) =
    sendWorldEvent(
        world,
        pos,
        payload,
        distance = 4.0 * SharedConstants.CHUNK_WIDTH,
        sourcePlayer = null,
    )

/**
 * This is mostly copied from [ServerWorld.syncWorldEvent], except we send our own custom payloads
 * instead of the default [WorldEventS2CPacket].
 */
fun sendWorldEvent(
    world: World,
    pos: BlockPos,
    payload: ModNetworkingPayload,
    distance: Double,
    sourcePlayer: PlayerEntity?,
) {
  val targetPlayers = world.server?.playerManager?.playerList
  if (targetPlayers == null) {
    return
  }

  for (targetPlayer in targetPlayers) {
    if (targetPlayer == sourcePlayer || targetPlayer.world.registryKey != world.registryKey) {
      continue
    }
    if (
        (pos.x - targetPlayer.x).pow(2) +
            (pos.y - targetPlayer.y).pow(2) +
            (pos.z - targetPlayer.z).pow(2) >= distance.pow(2)
    ) {
      continue
    }
    ServerPlayNetworking.send(targetPlayer, payload)
  }
}
