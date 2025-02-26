package com.vanillastar.vsbrewing.block

import com.vanillastar.vsbrewing.item.ModItemMetadata
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.item.BlockItem

data class ModBlockMetadata(
    /** Name for this [Block]. */
    val name: String,
    /** Settings for this [Block]. */
    val settingsProvider: (AbstractBlock.Settings) -> AbstractBlock.Settings,
    /** Metadata for this [BlockItem]. */
    val itemMetadata: ModItemMetadata?,
) {
  constructor(
      name: String,
      settingsProvider: (AbstractBlock.Settings) -> AbstractBlock.Settings,
  ) : this(name, settingsProvider, /* itemMetadata= */ null)
}
