package com.vanillastar.vsbrewing.block.entity

import net.minecraft.block.Block
import net.minecraft.block.entity.BlockEntity

data class ModBlockEntityMetadata(
    /** Name for this [BlockEntity]. */
    val name: String,
    /** List of [Block]'s that use this [BlockEntity]. */
    val blocks: List<Block>,
) {
  constructor(name: String, vararg blocks: Block) : this(name, listOf(*blocks))
}
