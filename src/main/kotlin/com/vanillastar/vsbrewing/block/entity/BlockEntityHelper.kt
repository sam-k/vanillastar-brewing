package com.vanillastar.vsbrewing.block.entity

import net.minecraft.SharedConstants
import net.minecraft.block.Block
import net.minecraft.block.entity.BlockEntity

const val BREWING_STAND_INVENTORY_SIZE = 5

const val BREWING_STAND_INVENTORY_INGREDIENT_SLOT_INDEX = 3

const val BREWING_STAND_INVENTORY_FUEL_SLOT_INDEX = 4

const val BREWING_CAULDRON_BREW_TIME_TICKS = 10 * SharedConstants.TICKS_PER_SECOND

data class ModBlockEntityMetadata(
    /** Name for this [BlockEntity]. */
    val name: String,
    /** List of [Block]'s that use this [BlockEntity]. */
    val blocks: List<Block>,
) {
  constructor(name: String, vararg blocks: Block) : this(name, listOf(*blocks))
}
