package com.vanillastar.vsbrewing.item

import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.registry.RegistryKey

data class ModItemGroupVisibilityMetadata(
    /** Visibility of this [Item] in the creative-mode inventory. */
    val visibility: ItemGroup.StackVisibility,
    /** [ItemStack] corresponding to this [Item]'s specified visibility. */
    val stackProvider: (ItemStack) -> Unit,
)

data class ModItemMetadata(
    /** Name for this [Item]. */
    val name: String,
    /** [RegistryKey] for this [Item] in the creative-mode inventory. */
    val itemGroup: RegistryKey<ItemGroup>,
    /** [Item] which must come before this item in the creative-mode inventory. */
    val previousItem: Item?,
    /** Visibility behaviors for this [Item] in the creative-mode inventory. */
    val itemGroupVisibilities: List<ModItemGroupVisibilityMetadata>,
    /** Settings for this [Item]. */
    val settingsProvider: (Item.Settings) -> Item.Settings,
) {
  constructor(
      name: String,
      itemGroup: RegistryKey<ItemGroup>,
      previousItem: Item,
      settingsProvider: (Item.Settings) -> Item.Settings,
  ) : this(
      name,
      itemGroup,
      previousItem,
      listOf(ModItemGroupVisibilityMetadata(ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS) {}),
      settingsProvider,
  )
}
