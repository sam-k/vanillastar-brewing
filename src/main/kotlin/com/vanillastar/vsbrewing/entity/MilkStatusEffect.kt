package com.vanillastar.vsbrewing.entity

import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.InstantStatusEffect
import net.minecraft.entity.effect.StatusEffectCategory
import net.minecraft.util.math.ColorHelper.Argb

class MilkStatusEffect :
    InstantStatusEffect(
        StatusEffectCategory.NEUTRAL,
        Argb.getArgb(/* red= */ 250, /* green= */ 253, /* blue= */ 253),
    ) {
  override fun applyInstantEffect(
      source: Entity?,
      attacker: Entity?,
      target: LivingEntity,
      amplifier: Int,
      proximity: Double,
  ) {
    target.clearStatusEffects()
  }
}
