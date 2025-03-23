package com.vanillastar.vsbrewing.tag

import com.vanillastar.vsbrewing.utils.ModRegistry
import com.vanillastar.vsbrewing.utils.getModIdentifier
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.TagKey

abstract class ModTags : ModRegistry() {
  @JvmField
  val brewableCauldrons: TagKey<Block> =
      TagKey.of(RegistryKeys.BLOCK, getModIdentifier("brewable_cauldrons"))

  @JvmField
  val placeableBottles: TagKey<Item> =
      TagKey.of(RegistryKeys.ITEM, getModIdentifier("placeable_bottles"))

  @JvmField
  val placeableBottlesWithSneaking: TagKey<Item> =
      TagKey.of(RegistryKeys.ITEM, getModIdentifier("placeable_bottles_with_sneaking"))

  override fun initialize() {}
}

@JvmField val MOD_TAGS = object : ModTags() {}
