package com.vanillastar.vsbrewing.item

import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.registry.RegistryKey

data class ModItemMetadata(
    /** Name for this [Item]. */
    val name: String,
    /** [RegistryKey] for this [Item] in the creative-mode inventory. */
    val itemGroup: RegistryKey<ItemGroup>,
    /** Visibility behavior for this [Item] in the creative-mode inventory. */
    val itemGroupVisibility: ItemGroup.StackVisibility,
    /** Settings for this [Item]. */
    val settingsProvider: (Item.Settings) -> Item.Settings,
) {
  constructor(
      name: String,
      itemGroup: RegistryKey<ItemGroup>,
      settingsProvider: (Item.Settings) -> Item.Settings,
  ) : this(name, itemGroup, ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS, settingsProvider)
}
