package com.vanillastar.vsbrewing.item

import com.vanillastar.vsbrewing.component.MOD_COMPONENTS
import com.vanillastar.vsbrewing.potion.MILK_POTION_ID
import com.vanillastar.vsbrewing.potion.potionContentsMatchId
import com.vanillastar.vsbrewing.utils.ModRegistry
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.item.ClampedModelPredicateProvider
import net.minecraft.client.item.ModelPredicateProviderRegistry
import net.minecraft.client.world.ClientWorld
import net.minecraft.component.DataComponentTypes
import net.minecraft.entity.LivingEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.Identifier

@Environment(EnvType.CLIENT)
abstract class ModModelPredicateProviders : ModRegistry() {
  override fun initialize() {
    this.registerModelPredicateProviders(
        "content_type",
        { stack: ItemStack, world: ClientWorld?, entity: LivingEntity?, seed: Int ->
          when {
            potionContentsMatchId(stack.get(DataComponentTypes.POTION_CONTENTS), MILK_POTION_ID) ->
                1.0f
            else -> 0.0f
          }
        },
        Items.LINGERING_POTION,
        Items.POTION,
        Items.SPLASH_POTION,
        MOD_ITEMS.lingeringPotionFlaskItem,
        MOD_ITEMS.potionFlaskItem,
        MOD_ITEMS.splashPotionFlaskItem,
    )
    this.registerModelPredicateProviders(
        "remaining_uses",
        { stack: ItemStack, world: ClientWorld?, entity: LivingEntity?, seed: Int ->
          stack.getOrDefault(MOD_COMPONENTS.flaskRemainingUsesComponent, PotionFlaskItem.MAX_USES) /
              PotionFlaskItem.MAX_USES.toFloat()
        },
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
