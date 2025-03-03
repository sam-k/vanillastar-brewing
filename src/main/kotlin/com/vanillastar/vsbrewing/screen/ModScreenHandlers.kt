package com.vanillastar.vsbrewing.screen

import com.vanillastar.vsbrewing.block.entity.BrewingCauldronStandBlockEntity
import com.vanillastar.vsbrewing.utils.ModRegistry
import com.vanillastar.vsbrewing.utils.getModIdentifier
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerType

abstract class ModScreenHandlers : ModRegistry() {
  @JvmField
  val brewingCauldronScreenHandler =
      this.registerExtendedScreenHandler(
          BREWING_CAULDRON_SCREEN_HANDLER_NAME,
          ::BrewingCauldronScreenHandler,
          BrewingCauldronStandBlockEntity.BrewingCauldronData.PACKET_CODEC,
      )

  @Suppress("SameParameterValue")
  private fun <
      TScreenHandler : ScreenHandler,
      TData : Any,
      TCodec : PacketCodec<in RegistryByteBuf, TData>,
  > registerExtendedScreenHandler(
      name: String,
      factory: ExtendedScreenHandlerType.ExtendedFactory<TScreenHandler, TData>,
      codec: TCodec,
  ): ScreenHandlerType<TScreenHandler> {
    val screenHandlerType =
        Registry.register(
            Registries.SCREEN_HANDLER,
            getModIdentifier(name),
            ExtendedScreenHandlerType<TScreenHandler, TData>(factory, codec),
        )
    this.logger.info("Registered screen handler {}", name)
    return screenHandlerType
  }

  override fun initialize() {}
}

@JvmField val MOD_SCREEN_HANDLERS = object : ModScreenHandlers() {}
