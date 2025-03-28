package com.vanillastar.vsbrewing.potion

import com.vanillastar.vsbrewing.utils.getModIdentifier
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.util.Identifier

fun potionContentsMatchId(potionContents: PotionContentsComponent?, id: Identifier) =
    potionContents?.potion?.orElse(null)?.matchesId(id) == true &&
        potionContents.customEffects.isEmpty()

@JvmField val MILK_POTION_ID = getModIdentifier("milk")

@JvmField val HUNGER_POTION_ID = getModIdentifier("hunger")
@JvmField val LONG_HUNGER_POTION_ID = getModIdentifier("long_hunger")
@JvmField val STRONG_HUNGER_POTION_ID = getModIdentifier("strong_hunger")

@JvmField val STRONG_WEAKNESS_POTION_ID = getModIdentifier("strong_weakness")
