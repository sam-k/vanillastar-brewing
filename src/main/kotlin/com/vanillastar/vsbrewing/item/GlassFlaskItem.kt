package com.vanillastar.vsbrewing.item

import net.minecraft.item.Item
import net.minecraft.item.ItemGroups

val GLASS_FLASK_ITEM_METADATA =
    ModItemMetadata("glass_flask", ItemGroups.INGREDIENTS) { it.maxCount(64) }

class GlassFlaskItem(settings: Settings) : Item(settings)
