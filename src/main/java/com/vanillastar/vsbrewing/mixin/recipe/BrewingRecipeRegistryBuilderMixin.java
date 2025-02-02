package com.vanillastar.vsbrewing.mixin.recipe;

import static com.vanillastar.vsbrewing.utils.LoggerHelperKt.getLogger;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.registry.entry.RegistryEntry;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrewingRecipeRegistry.Builder.class)
public abstract class BrewingRecipeRegistryBuilderMixin {
  @Unique
  private static final Logger LOGGER = getLogger();

  @Unique
  private static @NotNull String getRecipeString(
      RegistryEntry<Potion> input, Item ingredient, RegistryEntry<Potion> output) {
    return String.format("%s + %s = %s", input, ingredient, output);
  }

  @Inject(method = "registerPotionRecipe", at = @At("HEAD"), cancellable = true)
  private void removeBrewingRecipes(
      RegistryEntry<Potion> input, Item ingredient, RegistryEntry<Potion> output, CallbackInfo ci) {
    String recipeString = getRecipeString(input, ingredient, output);
    if (recipeString.equals(getRecipeString(Potions.WATER, Items.REDSTONE, Potions.MUNDANE))) {
      LOGGER.info("Unregistered brewing recipe: {}", recipeString);
      ci.cancel();
    }
  }
}
