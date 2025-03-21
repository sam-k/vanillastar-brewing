package com.vanillastar.vsbrewing.recipe

import com.vanillastar.vsbrewing.item.MOD_ITEMS
import com.vanillastar.vsbrewing.potion.LONG_NAUSEA_POTION_ID
import com.vanillastar.vsbrewing.potion.NAUSEA_POTION_ID
import com.vanillastar.vsbrewing.potion.STRONG_WEAKNESS_POTION_ID
import com.vanillastar.vsbrewing.utils.ModRegistry
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.potion.Potion
import net.minecraft.potion.Potions
import net.minecraft.registry.Registries
import net.minecraft.registry.entry.RegistryEntry

abstract class ModBrewingRecipes : ModRegistry() {
  override fun initialize() {
    FabricBrewingRecipeRegistryBuilder.BUILD.register {
      val nauseaPotion = Registries.POTION.getEntry(NAUSEA_POTION_ID).get()
      val longNauseaPotion = Registries.POTION.getEntry(LONG_NAUSEA_POTION_ID).get()
      val strongWeaknessPotion = Registries.POTION.getEntry(STRONG_WEAKNESS_POTION_ID).get()

      fun registerPotionType(item: Item) {
        it.registerPotionType(item)
        logger.info("Registered potion type {}", item)
      }

      fun registerPotionRecipe(
          input: RegistryEntry<Potion>,
          ingredient: Item,
          output: RegistryEntry<Potion>,
      ) {
        it.registerPotionRecipe(input, ingredient, output)
        logger.info(
            "Registered potion recipe: {} + {} = {}",
            input.idAsString,
            ingredient,
            output.idAsString,
        )
      }

      registerPotionType(MOD_ITEMS.potionFlaskItem)

      registerPotionRecipe(Potions.WATER, Items.ARMADILLO_SCUTE, Potions.MUNDANE)
      registerPotionRecipe(Potions.WATER, Items.REDSTONE, Potions.THICK)

      registerPotionRecipe(
          Potions.STRONG_LEAPING,
          Items.FERMENTED_SPIDER_EYE,
          Potions.STRONG_SLOWNESS,
      )

      registerPotionRecipe(Potions.AWKWARD, Items.ARMADILLO_SCUTE, nauseaPotion)
      registerPotionRecipe(nauseaPotion, Items.REDSTONE, longNauseaPotion)

      registerPotionRecipe(Potions.AWKWARD, Items.POISONOUS_POTATO, Potions.POISON)
      registerPotionRecipe(Potions.LONG_POISON, Items.FERMENTED_SPIDER_EYE, Potions.HARMING)

      registerPotionRecipe(Potions.REGENERATION, Items.FERMENTED_SPIDER_EYE, Potions.POISON)
      registerPotionRecipe(
          Potions.LONG_REGENERATION,
          Items.FERMENTED_SPIDER_EYE,
          Potions.LONG_POISON,
      )
      registerPotionRecipe(
          Potions.STRONG_REGENERATION,
          Items.FERMENTED_SPIDER_EYE,
          Potions.STRONG_POISON,
      )

      registerPotionRecipe(Potions.STRENGTH, Items.FERMENTED_SPIDER_EYE, Potions.WEAKNESS)
      registerPotionRecipe(Potions.LONG_STRENGTH, Items.FERMENTED_SPIDER_EYE, Potions.LONG_WEAKNESS)
      registerPotionRecipe(
          Potions.STRONG_STRENGTH,
          Items.FERMENTED_SPIDER_EYE,
          strongWeaknessPotion,
      )

      registerPotionRecipe(
          Potions.STRONG_SWIFTNESS,
          Items.FERMENTED_SPIDER_EYE,
          Potions.STRONG_SLOWNESS,
      )

      registerPotionRecipe(Potions.WEAKNESS, Items.GLOWSTONE_DUST, strongWeaknessPotion)
    }
  }
}

@JvmField val MOD_BREWING_RECIPES = object : ModBrewingRecipes() {}
