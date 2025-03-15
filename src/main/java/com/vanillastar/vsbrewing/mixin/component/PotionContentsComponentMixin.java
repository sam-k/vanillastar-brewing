package com.vanillastar.vsbrewing.mixin.component;

import java.util.List;
import java.util.Optional;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.registry.entry.RegistryEntry;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PotionContentsComponent.class)
public abstract class PotionContentsComponentMixin {
  @Inject(
      method =
          "createStack(Lnet/minecraft/item/Item;Lnet/minecraft/registry/entry/RegistryEntry;)Lnet/minecraft/item/ItemStack;",
      at = @At("HEAD"),
      cancellable = true)
  private static void createFailureStack(
      Item item, @NotNull RegistryEntry<Potion> potion, CallbackInfoReturnable<ItemStack> cir) {
    if (!potion.matchesKey(Potions.MUNDANE.getKey().orElse(null))
        && !potion.matchesKey(Potions.THICK.getKey().orElse(null))) {
      return;
    }

    ItemStack stack = new ItemStack(item);
    stack.set(
        DataComponentTypes.POTION_CONTENTS,
        new PotionContentsComponent(
            Optional.of(potion),
            Optional.of(PotionContentsComponent.getColor(List.of())),
            List.of()));
    cir.setReturnValue(stack);
    cir.cancel();
  }
}
