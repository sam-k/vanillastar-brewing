package com.vanillastar.vsbrewing.item

import com.vanillastar.vsbrewing.utils.ModRegistry
import com.vanillastar.vsbrewing.utils.getModIdentifier
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry

abstract class ModItems : ModRegistry() {
  @JvmField val glassFlaskItem = this.registerItem(GLASS_FLASK_ITEM_METADATA, ::GlassFlaskItem)
  @JvmField val potionFlaskItem = this.registerItem(POTION_FLASK_ITEM_METADATA, ::PotionFlaskItem)

  private fun registerItem(metadata: ModItemMetadata, constructor: (Item.Settings) -> Item): Item {
    val id = getModIdentifier(metadata.name)
    val item =
        Registry.register(
            Registries.ITEM,
            id,
            constructor(metadata.settingsProvider(Item.Settings())),
        )
    for ((visibility, stackProvider) in metadata.itemGroupVisibilities) {
      val stack = ItemStack(item)
      stackProvider(stack)
      ItemGroupEvents.modifyEntriesEvent(metadata.itemGroup).register {
        if (metadata.previousItem == null) {
          it.add(stack, visibility)
        } else {
          it.addAfter(metadata.previousItem, listOf(stack), visibility)
        }
      }
    }
    this.logger.info(
        "Registered item {} in group {}|{}",
        id,
        metadata.itemGroup.registry,
        metadata.itemGroup.value,
    )
    return item
  }

  override fun initialize() {}
}

@JvmField val MOD_ITEMS = object : ModItems() {}
