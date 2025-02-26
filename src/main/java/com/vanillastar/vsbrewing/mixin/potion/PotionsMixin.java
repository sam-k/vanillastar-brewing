package com.vanillastar.vsbrewing.mixin.potion;

import static com.vanillastar.vsbrewing.potion.StrongWeaknessPotionKt.STRONG_WEAKNESS_POTION_ID;
import static com.vanillastar.vsbrewing.utils.LoggerHelperKt.getMixinLogger;

import net.minecraft.SharedConstants;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Potions.class)
public abstract class PotionsMixin {
  @ModifyArgs(
      method = "<clinit>",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/potion/Potions;register(Ljava/lang/String;Lnet/minecraft/potion/Potion;)Lnet/minecraft/registry/entry/RegistryEntry;"))
  private static void registerPotions(@NotNull Args args) {
    // Inject before Potions.LUCK registration, which occurs immediately after Potions.LONG_WEAKNESS
    // registration.
    String name = args.get(0);
    if (!name.equals("luck")) {
      return;
    }
    Registry.registerReference(
        Registries.POTION,
        STRONG_WEAKNESS_POTION_ID,
        new Potion(
            "weakness",
            new StatusEffectInstance(
                StatusEffects.WEAKNESS,
                15 * SharedConstants.TICKS_PER_SECOND,
                /* amplifier= */ 1)));
    getMixinLogger().info("Registered potion {}", STRONG_WEAKNESS_POTION_ID);
  }
}
