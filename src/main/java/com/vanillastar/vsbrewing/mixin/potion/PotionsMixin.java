package com.vanillastar.vsbrewing.mixin.potion;

import static com.vanillastar.vsbrewing.entity.ModStatusEffectsKt.MOD_STATUS_EFFECTS;
import static com.vanillastar.vsbrewing.potion.ModPotionsKt.ARMADILLO_SCOURGE_POTION_BASENAME;
import static com.vanillastar.vsbrewing.potion.ModPotionsKt.ARMADILLO_SCOURGE_POTION_ID;
import static com.vanillastar.vsbrewing.potion.ModPotionsKt.LONG_ARMADILLO_SCOURGE_POTION_ID;
import static com.vanillastar.vsbrewing.potion.ModPotionsKt.MILK_POTION_BASENAME;
import static com.vanillastar.vsbrewing.potion.ModPotionsKt.MILK_POTION_ID;
import static com.vanillastar.vsbrewing.potion.ModPotionsKt.STRONG_ARMADILLO_SCOURGE_POTION_ID;
import static com.vanillastar.vsbrewing.potion.ModPotionsKt.STRONG_WEAKNESS_POTION_ID;
import static com.vanillastar.vsbrewing.utils.LoggerHelperKt.getMixinLogger;

import net.minecraft.SharedConstants;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Potions.class)
public abstract class PotionsMixin {
  @Unique
  private static void registerPotion(Identifier potionId, Potion potion) {
    Registry.registerReference(Registries.POTION, potionId, potion);
    getMixinLogger().info("Registered potion {}", potionId);
  }

  @ModifyArgs(
      method = "<clinit>",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/potion/Potions;register(Ljava/lang/String;Lnet/minecraft/potion/Potion;)Lnet/minecraft/registry/entry/RegistryEntry;"))
  private static void registerPotions(@NotNull Args args) {
    String name = args.get(0);
    switch (name) {
      case "mundane":
        args.set(
            1, // `Potion potion`
            new Potion(
                name,
                new StatusEffectInstance(
                    StatusEffects.NAUSEA, 5 * SharedConstants.TICKS_PER_SECOND)));
        break;

      case "thick":
        args.set(
            1, // `Potion potion`
            new Potion(
                name,
                new StatusEffectInstance(
                    StatusEffects.BLINDNESS, 5 * SharedConstants.TICKS_PER_SECOND)));
        break;

      case "night_vision":
        // Inject between registrations of `Potions.AWKWARD` and `Potions.NIGHT_VISION`.
        registerPotion(
            MILK_POTION_ID,
            new Potion(
                MILK_POTION_BASENAME,
                new StatusEffectInstance(MOD_STATUS_EFFECTS.milkStatusEffect, /* duration= */ 1)));
        break;

      case "luck":
        // Inject between registrations of `Potions.LONG_WEAKNESS` and `Potions.LUCK`.
        registerPotion(
            STRONG_WEAKNESS_POTION_ID,
            new Potion(
                /* baseName= */ "weakness",
                new StatusEffectInstance(
                    StatusEffects.WEAKNESS,
                    15 * SharedConstants.TICKS_PER_SECOND,
                    /* amplifier= */ 1)));
        registerPotion(
            ARMADILLO_SCOURGE_POTION_ID,
            new Potion(
                ARMADILLO_SCOURGE_POTION_BASENAME,
                new StatusEffectInstance(
                    StatusEffects.HUNGER, 30 * SharedConstants.TICKS_PER_SECOND),
                new StatusEffectInstance(
                    MOD_STATUS_EFFECTS.healthDownStatusEffect,
                    15 * SharedConstants.TICKS_PER_SECOND)));
        registerPotion(
            LONG_ARMADILLO_SCOURGE_POTION_ID,
            new Potion(
                ARMADILLO_SCOURGE_POTION_BASENAME,
                new StatusEffectInstance(
                    StatusEffects.HUNGER, 60 * SharedConstants.TICKS_PER_SECOND),
                new StatusEffectInstance(
                    MOD_STATUS_EFFECTS.healthDownStatusEffect,
                    30 * SharedConstants.TICKS_PER_SECOND)));
        registerPotion(
            STRONG_ARMADILLO_SCOURGE_POTION_ID,
            new Potion(
                ARMADILLO_SCOURGE_POTION_BASENAME,
                new StatusEffectInstance(
                    StatusEffects.HUNGER,
                    30 * SharedConstants.TICKS_PER_SECOND,
                    /* amplifier= */ 1),
                new StatusEffectInstance(
                    MOD_STATUS_EFFECTS.healthDownStatusEffect,
                    15 * SharedConstants.TICKS_PER_SECOND,
                    /* amplifier= */ 1)));
        break;
    }
  }
}
