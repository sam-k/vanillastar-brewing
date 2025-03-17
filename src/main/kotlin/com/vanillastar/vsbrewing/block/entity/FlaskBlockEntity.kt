package com.vanillastar.vsbrewing.block.entity

import com.vanillastar.vsbrewing.block.MOD_BLOCKS
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos

val FLASK_BLOCK_ENTITY_METADATA = ModBlockEntityMetadata("flask", MOD_BLOCKS.flaskBlock)

/** [BlockEntity] for a placed flask. */
class FlaskBlockEntity(
    pos: BlockPos,
    val state: BlockState,
    var itemStack: ItemStack = ItemStack.EMPTY,
) : BlockEntity(MOD_BLOCK_ENTITIES.flaskBlockEntityType, pos, state) {}
