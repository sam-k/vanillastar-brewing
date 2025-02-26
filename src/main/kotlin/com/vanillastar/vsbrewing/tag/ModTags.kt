package com.vanillastar.vsbrewing.tag

import com.vanillastar.vsbrewing.utils.ModRegistry
import com.vanillastar.vsbrewing.utils.getModIdentifier
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.TagKey

abstract class ModTags : ModRegistry() {
  val brewableCauldrons = TagKey.of(RegistryKeys.BLOCK, getModIdentifier("brewable_cauldrons"))

  override fun initialize() {}
}

@JvmField val MOD_TAGS = object : ModTags() {}
