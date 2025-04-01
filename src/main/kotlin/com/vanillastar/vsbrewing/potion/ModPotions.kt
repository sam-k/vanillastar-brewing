package com.vanillastar.vsbrewing.potion

import com.vanillastar.vsbrewing.utils.getModIdentifier
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.util.Identifier

fun potionContentsMatchId(potionContents: PotionContentsComponent?, id: Identifier) =
    potionContents?.potion?.orElse(null)?.matchesId(id) == true &&
        potionContents.customEffects.isEmpty()

const val MILK_POTION_BASENAME = "milk"
@JvmField val MILK_POTION_ID = getModIdentifier("milk")

const val ARMADILLO_SCOURGE_POTION_BASENAME = "armadillo_scourge"
@JvmField val ARMADILLO_SCOURGE_POTION_ID = getModIdentifier("armadillo_scourge")
@JvmField val LONG_ARMADILLO_SCOURGE_POTION_ID = getModIdentifier("long_armadillo_scourge")
@JvmField val STRONG_ARMADILLO_SCOURGE_POTION_ID = getModIdentifier("strong_armadillo_scourge")

@JvmField val STRONG_WEAKNESS_POTION_ID = getModIdentifier("strong_weakness")
