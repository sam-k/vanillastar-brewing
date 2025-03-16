package com.vanillastar.vsbrewing.block.entity

import net.minecraft.SharedConstants
import net.minecraft.block.Block
import net.minecraft.block.entity.BlockEntity

const val BREWING_STAND_INVENTORY_SIZE = 5

@JvmField val BREWING_STAND_INVENTORY_POTION_SLOT_INDICES = intArrayOf(0, 1, 2)

const val BREWING_CAULDRON_BREW_TIME_TICKS = 10 * SharedConstants.TICKS_PER_SECOND

data class ModBlockEntityMetadata(
    /** Name for this [BlockEntity]. */
    val name: String,
    /** List of [Block]'s that use this [BlockEntity]. */
    val blocks: List<Block>,
) {
  constructor(name: String, vararg blocks: Block) : this(name, listOf(*blocks))
}
