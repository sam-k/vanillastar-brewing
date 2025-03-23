package com.vanillastar.vsbrewing.component

import com.mojang.serialization.Codec
import com.vanillastar.vsbrewing.utils.ModRegistry
import com.vanillastar.vsbrewing.utils.getModIdentifier
import net.minecraft.component.ComponentType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry

abstract class ModComponents : ModRegistry() {
  @JvmField
  val potionFlaskRemainingUsesComponent =
      this.registerComponent("potion_flask_remaining_uses", Codec.INT)

  @Suppress("SameParameterValue")
  private fun <TCodec> registerComponent(
      name: String,
      codec: Codec<TCodec>,
  ): ComponentType<TCodec> {
    val component =
        Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            getModIdentifier(name),
            ComponentType.builder<TCodec>().codec(codec).build(),
        )
    this.logger.info("Registered component {}", name)
    return component
  }

  override fun initialize() {}
}

@JvmField val MOD_COMPONENTS = object : ModComponents() {}
