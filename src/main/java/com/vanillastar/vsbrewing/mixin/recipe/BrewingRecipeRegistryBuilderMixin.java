package com.vanillastar.vsbrewing.mixin.recipe;

import static com.vanillastar.vsbrewing.item.ModItemsKt.MOD_ITEMS;
import static com.vanillastar.vsbrewing.utils.LoggerHelperKt.getMixinLogger;

import java.util.Set;
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
  private static Set<String> DEREGISTERED_BREWING_RECIPES = Set.of(
      getRecipeString(Potions.WATER, Items.REDSTONE, Potions.MUNDANE),
      getRecipeString(Potions.AWKWARD, Items.TURTLE_HELMET, Potions.TURTLE_MASTER));

  @Unique
  private static final Logger LOGGER = getMixinLogger();

  @Unique
  private static @NotNull String getRecipeString(
      @NotNull RegistryEntry<Potion> input,
      Item ingredient,
      @NotNull RegistryEntry<Potion> output) {
    return String.format("%s + %s = %s", input.getIdAsString(), ingredient, output.getIdAsString());
  }

  @Inject(method = "registerPotionRecipe", at = @At("HEAD"), cancellable = true)
  private void removeBrewingRecipes(
      RegistryEntry<Potion> input, Item ingredient, RegistryEntry<Potion> output, CallbackInfo ci) {
    String recipeString = getRecipeString(input, ingredient, output);
    if (DEREGISTERED_BREWING_RECIPES.contains(recipeString)) {
      LOGGER.info("Deregistered brewing recipe: {}", recipeString);
      ci.cancel();
    }
  }

  @Inject(method = "assertPotion(Lnet/minecraft/item/Item;)V", at = @At("HEAD"), cancellable = true)
  private static void assertPotionFlask(@NotNull Item potionType, CallbackInfo ci) {
    if (potionType.equals(MOD_ITEMS.potionFlaskItem)) {
      ci.cancel();
    }
  }
}
