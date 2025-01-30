package com.vanillastar.vsbrewing.item

import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.item.ItemGroups
import net.minecraft.item.PotionItem

val POTION_FLASK_ITEM_METADATA =
    ModItemMetadata("potion_flask", ItemGroups.INGREDIENTS) {
      it.maxCount(1).component(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT)
    }

class PotionFlaskItem(settings: Settings) : PotionItem(settings) {}
