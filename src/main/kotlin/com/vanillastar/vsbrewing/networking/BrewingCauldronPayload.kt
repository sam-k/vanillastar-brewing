package com.vanillastar.vsbrewing.networking

import com.vanillastar.vsbrewing.screen.BrewingCauldronScreenHandler
import com.vanillastar.vsbrewing.screen.MOD_SCREEN_HANDLERS
import com.vanillastar.vsbrewing.utils.getModIdentifier
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload

data class BrewingCauldronPayload(
    val packedPos: Long,
    val level: Int,
    val potionCauldronNbt: NbtCompound,
) : ModNetworkingPayload.Server<BrewingCauldronPayload> {
  companion object : ModNetworkingPayload.ServerCompanion<BrewingCauldronPayload> {
    val BREWING_CAULDRON_PACKET_ID = getModIdentifier("brewing_cauldron")

    override val id: CustomPayload.Id<BrewingCauldronPayload> =
        CustomPayload.Id(BREWING_CAULDRON_PACKET_ID)

    override val codec: PacketCodec<RegistryByteBuf, BrewingCauldronPayload> =
        PacketCodec.tuple(
            PacketCodecs.VAR_LONG,
            BrewingCauldronPayload::packedPos,
            PacketCodecs.INTEGER,
            BrewingCauldronPayload::level,
            PacketCodecs.NBT_COMPOUND,
            BrewingCauldronPayload::potionCauldronNbt,
            ::BrewingCauldronPayload,
        )

    override val callback: ModNetworkingClientCallback<BrewingCauldronPayload> =
        { payload, context ->
          {
            val screenHandler = context.player().currentScreenHandler
            if (
                screenHandler.type == MOD_SCREEN_HANDLERS.brewingCauldronScreenHandler &&
                    screenHandler is BrewingCauldronScreenHandler
            ) {
              screenHandler.data = payload
            }
          }
        }
  }

  override val companion = Companion

  override fun getId(): CustomPayload.Id<BrewingCauldronPayload> = companion.id
}
