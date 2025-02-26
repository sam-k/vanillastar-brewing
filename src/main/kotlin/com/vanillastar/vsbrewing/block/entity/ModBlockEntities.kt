package com.vanillastar.vsbrewing.block.entity

import com.vanillastar.vsbrewing.utils.ModRegistry
import com.vanillastar.vsbrewing.utils.getModIdentifier
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.math.BlockPos

abstract class ModBlockEntities : ModRegistry() {
  @JvmField
  val brewingCauldronStandBlockEntityType =
      registerBlockEntity(
          BREWING_CAULDRON_STAND_BLOCK_ENTITY_METADATA,
          ::BrewingCauldronStandBlockEntity,
      )

  @JvmField
  val potionCauldronBlockEntityType =
      registerBlockEntity(POTION_CAULDRON_BLOCK_ENTITY_METADATA, ::PotionCauldronBlockEntity)

  private fun <TBlockEntity : BlockEntity> registerBlockEntity(
      metadata: ModBlockEntityMetadata,
      constructor: (BlockPos, BlockState) -> TBlockEntity,
  ): BlockEntityType<TBlockEntity> {
    val blockEntityType =
        Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            getModIdentifier(metadata.name),
            BlockEntityType.Builder.create(constructor, *metadata.blocks.toTypedArray()).build(),
        )
    logger.info(
        "Registered block entity {} for blocks {}",
        metadata.name,
        metadata.blocks.map { it.name }.joinToString(),
    )
    return blockEntityType
  }

  override fun initialize() {}
}

@JvmField val MOD_BLOCK_ENTITIES = object : ModBlockEntities() {}
