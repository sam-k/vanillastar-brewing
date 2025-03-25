package com.vanillastar.vsbrewing.block

import net.minecraft.block.Block
import net.minecraft.block.LeveledCauldronBlock
import net.minecraft.block.MapColor
import net.minecraft.world.biome.Biome

val MILK_CAULDRON_BLOCK_METADATA =
    ModBlockMetadata("milk_cauldron") {
      it.mapColor(MapColor.STONE_GRAY).requiresTool().strength(2.0f)
    }

/** [Block] for a milk-filled cauldron. */
class MilkCauldronBlock(settings: Settings) :
    LeveledCauldronBlock(
        Biome.Precipitation.NONE,
        MOD_CAULDRON_BEHAVIORS.milkCauldronBehavior,
        settings,
    ) {}
