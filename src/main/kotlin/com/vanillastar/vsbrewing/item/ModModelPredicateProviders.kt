package com.vanillastar.vsbrewing.item

import com.vanillastar.vsbrewing.component.MOD_COMPONENTS
import com.vanillastar.vsbrewing.utils.ModRegistry
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.item.ClampedModelPredicateProvider
import net.minecraft.client.item.ModelPredicateProviderRegistry
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.LivingEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier

@Environment(EnvType.CLIENT)
abstract class ModModelPredicateProviders : ModRegistry() {
  override fun initialize() {
    this.registerModelPredicateProviders(
        "remaining_uses",
        { stack: ItemStack, world: ClientWorld?, entity: LivingEntity?, seed: Int ->
          stack.getOrDefault(
              MOD_COMPONENTS.flaskRemainingUsesComponent,
              DrinkableFlaskItem.MAX_USES,
          ) / DrinkableFlaskItem.MAX_USES.toFloat()
        },
        MOD_ITEMS.milkFlaskItem,
        MOD_ITEMS.potionFlaskItem,
    )
  }

  @Suppress("SameParameterValue")
  private fun registerModelPredicateProviders(
      name: String,
      provider: ClampedModelPredicateProvider,
      vararg items: Item,
  ) {
    // This identifier should not be namespaced.
    for (item in items) {
      ModelPredicateProviderRegistry.register(item, Identifier.ofVanilla(name), provider)
    }
    this.logger.info(
        "Registered model predicate provider {} for items {}",
        name,
        items.joinToString(", "),
    )
  }
}

@JvmField val MOD_MODEL_PREDICATE_PROVIDERS = object : ModModelPredicateProviders() {}
