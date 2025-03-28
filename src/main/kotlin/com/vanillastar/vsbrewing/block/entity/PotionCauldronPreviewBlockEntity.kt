package com.vanillastar.vsbrewing.block.entity

import com.vanillastar.vsbrewing.block.MOD_BLOCKS
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.util.math.BlockPos

val POTION_CAULDRON_PREVIEW_BLOCK_ENTITY_METADATA =
    ModBlockEntityMetadata("potion_cauldron_preview", MOD_BLOCKS.potionCauldronPreviewBlock)

/** [BlockEntity] for a potion-filled cauldron. */
class PotionCauldronPreviewBlockEntity(
    pos: BlockPos,
    state: BlockState,
    potionContents: PotionContentsComponent = PotionContentsComponent.DEFAULT,
    forcedLevel: Int? = null,
) :
    AbstractPotionCauldronBlockEntity(
        MOD_BLOCK_ENTITIES.potionCauldronPreviewBlockEntityType,
        pos,
        state,
        potionContents,
        forcedLevel,
    )
