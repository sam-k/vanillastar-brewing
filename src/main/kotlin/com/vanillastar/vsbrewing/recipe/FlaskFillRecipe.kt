package com.vanillastar.vsbrewing.recipe

import com.vanillastar.vsbrewing.component.MOD_COMPONENTS
import com.vanillastar.vsbrewing.item.FLASK_MAX_USES
import com.vanillastar.vsbrewing.item.FLASK_MIN_USES
import com.vanillastar.vsbrewing.item.MOD_ITEMS
import com.vanillastar.vsbrewing.tag.MOD_TAGS
import com.vanillastar.vsbrewing.utils.PotionVariant
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.potion.Potion
import net.minecraft.recipe.SpecialCraftingRecipe
import net.minecraft.recipe.book.CraftingRecipeCategory
import net.minecraft.recipe.input.CraftingRecipeInput
import net.minecraft.registry.RegistryWrapper.WrapperLookup
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.util.collection.DefaultedList
import net.minecraft.world.World

/**
 * [SpecialCraftingRecipe] for filling a flask with one or more matching potions, depending on the
 * existing flask level.
 */
class FlaskFillRecipe(category: CraftingRecipeCategory) : SpecialCraftingRecipe(category) {
  private companion object {
    /**
     * Gets the output flask's new level, variant and [Potion] entry, or `null` if the recipe is
     * invalid.
     */
    fun getCraftingResults(
        input: CraftingRecipeInput
    ): Triple<Int, PotionVariant, RegistryEntry<Potion>>? {
      var flaskStack: ItemStack? = null
      var potionVariant: PotionVariant? = null
      val potionContentsList = mutableListOf<PotionContentsComponent?>()
      for (stack in input.stacks) {
        if (stack.isOf(MOD_ITEMS.glassFlaskItem) || stack.isOf(MOD_ITEMS.potionFlaskItem)) {
          if (flaskStack != null) {
            // Recipe already contains a flask.
            return null
          }
          flaskStack = stack
        } else if (stack.isIn(MOD_TAGS.potionBottles)) {
          val newPotionVariant = PotionVariant.get(stack)
          if (
              newPotionVariant == null ||
                  (potionVariant != null && potionVariant != newPotionVariant)
          ) {
            // Recipe contains invalid or mismatched potion variants.
            return null
          }
          potionVariant = newPotionVariant
          potionContentsList.add(stack.get(DataComponentTypes.POTION_CONTENTS))
        } else if (!stack.isEmpty) {
          // Recipe contains foreign items.
          return null
        }
      }
      if (flaskStack == null || potionVariant == null || potionContentsList.isEmpty()) {
        // Recipe is incomplete.
        return null
      }

      val newLevel =
          flaskStack.getOrDefault(MOD_COMPONENTS.flaskRemainingUsesComponent, 0) +
              potionContentsList.size
      if (
          newLevel < FLASK_MIN_USES ||
              newLevel > FLASK_MAX_USES ||
              potionContentsList.any { it == null }
      ) {
        // Recipe is invalid.
        return null
      }
      if (potionVariant != PotionVariant.NORMAL && newLevel != FLASK_MAX_USES) {
        // Splash and lingering flasks cannot be partially full.
        return null
      }

      val referencePotionEntry =
          if (flaskStack.contains(DataComponentTypes.POTION_CONTENTS)) {
                flaskStack.get(DataComponentTypes.POTION_CONTENTS)
              } else {
                potionContentsList.removeFirst()
              }
              ?.potion
              ?.orElse(null)
      if (
          referencePotionEntry == null ||
              potionContentsList.any { it?.matches(referencePotionEntry) != true }
      ) {
        // Recipe contains no or mismatched potions.
        return null
      }

      return Triple(newLevel, potionVariant, referencePotionEntry)
    }
  }

  override fun matches(input: CraftingRecipeInput, world: World) = getCraftingResults(input) != null

  override fun craft(input: CraftingRecipeInput, lookup: WrapperLookup): ItemStack {
    val parts = getCraftingResults(input)
    if (parts == null) {
      return ItemStack.EMPTY
    }

    val (newLevel, potionVariant, potionEntry) = parts
    val newStack =
        PotionContentsComponent.createStack(
            PotionVariant.getItem(potionVariant, isFlask = true),
            potionEntry,
        )
    if (potionVariant == PotionVariant.NORMAL) {
      newStack.set(MOD_COMPONENTS.flaskRemainingUsesComponent, newLevel)
    }
    return newStack
  }

  override fun getRemainder(input: CraftingRecipeInput): DefaultedList<ItemStack> {
    val remainders = DefaultedList.ofSize(input.size, /* defaultValue= */ ItemStack.EMPTY)
    for (i in remainders.indices) {
      val stack = input.getStackInSlot(i)
      if (stack.isOf(Items.POTION)) {
        remainders[i] = Items.GLASS_BOTTLE.defaultStack
      }
    }
    return remainders
  }

  override fun fits(width: Int, height: Int) = width * height >= 2

  override fun getResult(registriesLookup: WrapperLookup) = ItemStack(MOD_ITEMS.potionFlaskItem)

  override fun getSerializer() = MOD_RECIPE_SERIALIZERS.flaskFillRecipeSerializer
}
