package com.vanillastar.vsbrewing.recipe

import com.vanillastar.vsbrewing.component.MOD_COMPONENTS
import com.vanillastar.vsbrewing.item.FLASK_MIN_USES
import com.vanillastar.vsbrewing.item.MOD_ITEMS
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

/** [SpecialCraftingRecipe] for emptying one level of a normal flask into a glass bottle. */
class FlaskEmptyRecipe(category: CraftingRecipeCategory) : SpecialCraftingRecipe(category) {
  private companion object {
    /**
     * Gets the input flask's new level, variant and [Potion] entry, or `null` if the recipe is
     * invalid.
     */
    fun getCraftingResults(
        input: CraftingRecipeInput
    ): Triple<Int, PotionVariant, RegistryEntry<Potion>>? {
      var flaskStack: ItemStack? = null
      val bottleStacks = mutableListOf<ItemStack>()
      for (stack in input.stacks) {
        if (stack.isOf(MOD_ITEMS.potionFlaskItem)) {
          if (flaskStack != null) {
            // Recipe already contains a flask.
            return null
          }
          flaskStack = stack
        } else if (stack.isOf(Items.GLASS_BOTTLE)) {
          bottleStacks.add(stack)
        } else if (!stack.isEmpty) {
          // Recipe contains foreign items.
          return null
        }
      }
      if (flaskStack == null || bottleStacks.isEmpty()) {
        // Recipe is invalid.
        return null
      }

      val potionVariant = PotionVariant.get(flaskStack)
      if (potionVariant != PotionVariant.NORMAL) {
        // Splash or lingering flasks cannot be emptied.
        return null
      }

      val potionEntry = flaskStack.get(DataComponentTypes.POTION_CONTENTS)?.potion?.orElse(null)
      if (potionEntry == null) {
        // Recipe contains no potions.
        return null
      }

      return Triple(
          flaskStack.getOrDefault(MOD_COMPONENTS.flaskRemainingUsesComponent, 0) - 1,
          potionVariant,
          potionEntry,
      )
    }
  }

  override fun matches(input: CraftingRecipeInput, world: World) = getCraftingResults(input) != null

  override fun craft(input: CraftingRecipeInput, lookup: WrapperLookup): ItemStack {
    val parts = getCraftingResults(input)
    if (parts == null) {
      return ItemStack.EMPTY
    }
    val (_, potionVariant, potionEntry) = parts
    return PotionContentsComponent.createStack(
        PotionVariant.getItem(potionVariant, isFlask = false),
        potionEntry,
    )
  }

  override fun getRemainder(input: CraftingRecipeInput): DefaultedList<ItemStack> {
    val remainders = DefaultedList.ofSize(input.size, /* defaultValue= */ ItemStack.EMPTY)
    val parts = getCraftingResults(input)
    if (parts == null) {
      return remainders
    }
    val (newLevel) = parts

    for (i in remainders.indices) {
      val stack = input.getStackInSlot(i)
      if (!stack.isOf(MOD_ITEMS.potionFlaskItem)) {
        continue
      }
      remainders[i] =
          if (newLevel >= FLASK_MIN_USES) {
            val newStack = stack.copy()
            newStack.set(MOD_COMPONENTS.flaskRemainingUsesComponent, newLevel)
            newStack
          } else {
            MOD_ITEMS.glassFlaskItem.defaultStack
          }
    }
    return remainders
  }

  override fun fits(width: Int, height: Int) = width * height >= 2

  override fun getResult(registriesLookup: WrapperLookup) = ItemStack(Items.POTION)

  override fun getSerializer() = MOD_RECIPE_SERIALIZERS.flaskEmptyRecipeSerializer
}
