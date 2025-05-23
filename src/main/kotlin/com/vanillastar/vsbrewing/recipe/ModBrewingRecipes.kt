package com.vanillastar.vsbrewing.recipe

import com.vanillastar.vsbrewing.item.MOD_ITEMS
import com.vanillastar.vsbrewing.potion.ARMADILLO_SCOURGE_POTION_ID
import com.vanillastar.vsbrewing.potion.LONG_ARMADILLO_SCOURGE_POTION_ID
import com.vanillastar.vsbrewing.potion.STRONG_ARMADILLO_SCOURGE_POTION_ID
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
      val armadilloScourgePotion = Registries.POTION.getEntry(ARMADILLO_SCOURGE_POTION_ID).get()
      val longArmadilloScourgePotion =
          Registries.POTION.getEntry(LONG_ARMADILLO_SCOURGE_POTION_ID).get()
      val strongArmadilloScourgePotion =
          Registries.POTION.getEntry(STRONG_ARMADILLO_SCOURGE_POTION_ID).get()
      val strongWeaknessPotion = Registries.POTION.getEntry(STRONG_WEAKNESS_POTION_ID).get()

      fun registerPotionType(item: Item) {
        it.registerPotionType(item)
        this.logger.info("Registered potion type {}", item)
      }

      fun registerItemRecipe(input: Item, ingredient: Item, output: Item) {
        it.registerItemRecipe(input, ingredient, output)
        this.logger.info("Registered item brewing recipe: {} + {} = {}", input, ingredient, output)
      }

      fun registerBrewingRecipe(
          input: RegistryEntry<Potion>,
          ingredient: Item,
          output: RegistryEntry<Potion>,
      ) {
        it.registerPotionRecipe(input, ingredient, output)
        this.logger.info(
            "Registered potion brewing recipe: {} + {} = {}",
            input.idAsString,
            ingredient,
            output.idAsString,
        )
      }

      registerPotionType(MOD_ITEMS.potionFlaskItem)
      registerPotionType(MOD_ITEMS.splashPotionFlaskItem)
      registerPotionType(MOD_ITEMS.lingeringPotionFlaskItem)

      registerItemRecipe(
          MOD_ITEMS.potionFlaskItem,
          Items.GUNPOWDER,
          MOD_ITEMS.splashPotionFlaskItem,
      )
      registerItemRecipe(
          MOD_ITEMS.splashPotionFlaskItem,
          Items.DRAGON_BREATH,
          MOD_ITEMS.lingeringPotionFlaskItem,
      )

      registerBrewingRecipe(Potions.WATER, Items.ARMADILLO_SCUTE, Potions.MUNDANE)
      registerBrewingRecipe(Potions.WATER, Items.REDSTONE, Potions.THICK)

      registerBrewingRecipe(
          Potions.STRONG_LEAPING,
          Items.FERMENTED_SPIDER_EYE,
          Potions.STRONG_SLOWNESS,
      )

      registerBrewingRecipe(Potions.AWKWARD, Items.ARMADILLO_SCUTE, armadilloScourgePotion)
      registerBrewingRecipe(armadilloScourgePotion, Items.REDSTONE, longArmadilloScourgePotion)
      registerBrewingRecipe(
          armadilloScourgePotion,
          Items.GLOWSTONE_DUST,
          strongArmadilloScourgePotion,
      )

      registerBrewingRecipe(Potions.AWKWARD, Items.POISONOUS_POTATO, Potions.POISON)
      registerBrewingRecipe(Potions.LONG_POISON, Items.FERMENTED_SPIDER_EYE, Potions.HARMING)

      registerBrewingRecipe(Potions.REGENERATION, Items.FERMENTED_SPIDER_EYE, Potions.POISON)
      registerBrewingRecipe(
          Potions.LONG_REGENERATION,
          Items.FERMENTED_SPIDER_EYE,
          Potions.LONG_POISON,
      )
      registerBrewingRecipe(
          Potions.STRONG_REGENERATION,
          Items.FERMENTED_SPIDER_EYE,
          Potions.STRONG_POISON,
      )

      registerBrewingRecipe(Potions.AWKWARD, Items.TURTLE_SCUTE, Potions.TURTLE_MASTER)

      registerBrewingRecipe(Potions.STRENGTH, Items.FERMENTED_SPIDER_EYE, Potions.WEAKNESS)
      registerBrewingRecipe(
          Potions.LONG_STRENGTH,
          Items.FERMENTED_SPIDER_EYE,
          Potions.LONG_WEAKNESS,
      )
      registerBrewingRecipe(
          Potions.STRONG_STRENGTH,
          Items.FERMENTED_SPIDER_EYE,
          strongWeaknessPotion,
      )

      registerBrewingRecipe(
          Potions.STRONG_SWIFTNESS,
          Items.FERMENTED_SPIDER_EYE,
          Potions.STRONG_SLOWNESS,
      )

      registerBrewingRecipe(Potions.WEAKNESS, Items.GLOWSTONE_DUST, strongWeaknessPotion)
    }
  }
}

@JvmField val MOD_BREWING_RECIPES = object : ModBrewingRecipes() {}
