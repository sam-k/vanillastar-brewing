package com.vanillastar.vsbrewing.entity

import com.vanillastar.vsbrewing.utils.getModIdentifier
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectCategory
import net.minecraft.util.math.ColorHelper.Argb

class HealthDownStatusEffect :
    StatusEffect(
        StatusEffectCategory.HARMFUL,
        Argb.getArgb(/* red= */ 7, /* green= */ 130, /* blue= */ 220),
    ) {
  init {
    this.addAttributeModifier(
        EntityAttributes.GENERIC_MAX_HEALTH,
        getModIdentifier("effect.health_down"),
        /* amount= */ -4.0,
        EntityAttributeModifier.Operation.ADD_VALUE,
    )
  }
}
