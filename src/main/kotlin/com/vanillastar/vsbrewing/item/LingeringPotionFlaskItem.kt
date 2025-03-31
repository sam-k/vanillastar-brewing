package com.vanillastar.vsbrewing.item

import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemGroups
import net.minecraft.item.LingeringPotionItem
import net.minecraft.registry.Registries

val LINGERING_POTION_FLASK_ITEM_METADATA =
    ModItemMetadata(
        "lingering_potion_flask",
        ItemGroups.FOOD_AND_DRINK,
        /* previousItem= */ null,
        Registries.POTION.streamEntries()
            .map {
              ModItemGroupVisibilityMetadata(
                  ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS,
                  getPotionFlaskStackProvider(it),
              )
            }
            .toList(),
    ) {
      it.maxCount(1).component(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT)
    }

class LingeringPotionFlaskItem(settings: Settings) : LingeringPotionItem(settings)
