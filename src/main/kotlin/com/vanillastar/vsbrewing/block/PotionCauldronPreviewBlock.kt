package com.vanillastar.vsbrewing.block

import net.minecraft.block.MapColor

val POTION_CAULDRON_PREVIEW_BLOCK_METADATA =
    ModBlockMetadata("potion_cauldron_preview") {
      it.mapColor(MapColor.STONE_GRAY)
          .strength(/* hardness= */ -1.0f, /* resistance= */ 3600000.0f)
          .dropsNothing()
    }

/** [PotionCauldronBlock] with two cauldron sides removed to show the potion contents inside. */
class PotionCauldronPreviewBlock(settings: Settings) : PotionCauldronBlock(settings)
