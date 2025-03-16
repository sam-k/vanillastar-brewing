package com.vanillastar.vsbrewing.block

import com.vanillastar.vsbrewing.utils.ModRegistry
import com.vanillastar.vsbrewing.utils.getModIdentifier
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.state.property.BooleanProperty

@JvmField val BREWING_STAND_IS_ON_CAULDRON: BooleanProperty = BooleanProperty.of("is_on_cauldron")

abstract class ModBlocks : ModRegistry() {
  @JvmField
  val potionCauldronBlock = registerBlock(POTION_CAULDRON_BLOCK_METADATA, ::PotionCauldronBlock)

  @JvmField
  val potionCauldronPreviewBlock =
      registerBlock(POTION_CAULDRON_PREVIEW_BLOCK_METADATA, ::PotionCauldronPreviewBlock)

  private fun <TBlock : Block> registerBlock(
      metadata: ModBlockMetadata,
      constructor: (AbstractBlock.Settings) -> TBlock,
  ): TBlock {
    val block =
        Registry.register(
            Registries.BLOCK,
            getModIdentifier(metadata.name),
            constructor(metadata.settingsProvider(AbstractBlock.Settings.create())),
        )
    logger.info("Registered block {}", metadata.name)

    if (metadata.itemMetadata != null) {
      val blockItem =
          Registry.register(
              Registries.ITEM,
              getModIdentifier(metadata.name),
              BlockItem(block, metadata.itemMetadata.settingsProvider(Item.Settings())),
          )
      ItemGroupEvents.modifyEntriesEvent(metadata.itemMetadata.itemGroup).register {
        it.add(blockItem)
      }
      logger.info(
          "Registered block item {} in group {}|{}",
          metadata.itemMetadata.name,
          metadata.itemMetadata.itemGroup.registry,
          metadata.itemMetadata.itemGroup.value,
      )
    }

    return block
  }

  override fun initialize() {}
}

@JvmField val MOD_BLOCKS = object : ModBlocks() {}
