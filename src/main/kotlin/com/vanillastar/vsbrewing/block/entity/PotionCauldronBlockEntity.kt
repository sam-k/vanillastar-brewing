package com.vanillastar.vsbrewing.block.entity

import com.vanillastar.vsbrewing.block.MOD_BLOCKS
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.util.math.BlockPos

val POTION_CAULDRON_BLOCK_ENTITY_METADATA =
    ModBlockEntityMetadata("potion_cauldron", MOD_BLOCKS.potionCauldronBlock)

/** [BlockEntity] for a potion-filled cauldron. */
class PotionCauldronBlockEntity(
    pos: BlockPos,
    state: BlockState,
    potionContents: PotionContentsComponent,
    variant: PotionCauldronVariant,
) :
    AbstractPotionCauldronBlockEntity(
        MOD_BLOCK_ENTITIES.potionCauldronBlockEntityType,
        pos,
        state,
        potionContents,
        variant,
    ) {
  constructor(
      pos: BlockPos,
      state: BlockState,
  ) : this(pos, state, PotionContentsComponent.DEFAULT, PotionCauldronVariant.NORMAL)
}
