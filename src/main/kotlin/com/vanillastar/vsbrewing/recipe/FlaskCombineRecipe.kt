package com.vanillastar.vsbrewing.recipe

import com.vanillastar.vsbrewing.component.MOD_COMPONENTS
import com.vanillastar.vsbrewing.item.FLASK_MAX_USES
import com.vanillastar.vsbrewing.item.FLASK_MIN_USES
import com.vanillastar.vsbrewing.item.MOD_ITEMS
import kotlin.math.max
import kotlin.math.min
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.item.ItemStack
import net.minecraft.potion.Potion
import net.minecraft.recipe.SpecialCraftingRecipe
import net.minecraft.recipe.book.CraftingRecipeCategory
import net.minecraft.recipe.input.CraftingRecipeInput
import net.minecraft.registry.RegistryWrapper.WrapperLookup
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.util.collection.DefaultedList
import net.minecraft.world.World

/** [SpecialCraftingRecipe] for combining the levels of two matching normal flasks. */
class FlaskCombineRecipe(category: CraftingRecipeCategory) : SpecialCraftingRecipe(category) {
  private companion object {
    const val COUNT = 2

    /**
     * Gets the input flasks' combined level and [Potion] entry, or `null` if the recipe is invalid.
     */
    fun getCraftingResults(input: CraftingRecipeInput): Pair<Int, RegistryEntry<Potion>>? {
      val flaskStacks = mutableListOf<ItemStack>()
      for (stack in input.stacks) {
        if (stack.isOf(MOD_ITEMS.potionFlaskItem)) {
          flaskStacks.add(stack)
        } else if (!stack.isEmpty) {
          // Recipe contains foreign items.
          return null
        }
      }
      if (flaskStacks.size != COUNT) {
        // Recipe is incomplete.
        return null
      }

      val totalLevel =
          flaskStacks.map { it.getOrDefault(MOD_COMPONENTS.flaskRemainingUsesComponent, 0) }.sum()
      if (
          totalLevel < COUNT * FLASK_MIN_USES ||
              totalLevel > COUNT * (FLASK_MAX_USES - 1) ||
              flaskStacks.any { !it.contains(DataComponentTypes.POTION_CONTENTS) }
      ) {
        // Recipe is invalid.
        return null
      }

      val referencePotionEntry =
          flaskStacks.removeFirst().get(DataComponentTypes.POTION_CONTENTS)?.potion?.orElse(null)
      if (
          referencePotionEntry == null ||
              flaskStacks.any {
                it.get(DataComponentTypes.POTION_CONTENTS)?.matches(referencePotionEntry) != true
              }
      ) {
        // Recipe contains no or mismatched potions.
        return null
      }

      return Pair(totalLevel, referencePotionEntry)
    }
  }

  override fun matches(input: CraftingRecipeInput, world: World) = getCraftingResults(input) != null

  override fun craft(input: CraftingRecipeInput, lookup: WrapperLookup): ItemStack {
    val parts = getCraftingResults(input)
    if (parts == null) {
      return ItemStack.EMPTY
    }
    val (totalLevel, potionEntry) = parts

    val newStack = PotionContentsComponent.createStack(MOD_ITEMS.potionFlaskItem, potionEntry)
    newStack.set(MOD_COMPONENTS.flaskRemainingUsesComponent, min(totalLevel, FLASK_MAX_USES))
    return newStack
  }

  override fun getRemainder(input: CraftingRecipeInput): DefaultedList<ItemStack> {
    val remainders = DefaultedList.ofSize(input.size, /* defaultValue= */ ItemStack.EMPTY)
    val parts = getCraftingResults(input)
    if (parts == null) {
      return remainders
    }
    var (totalLevel) = parts

    var hasConsumedFlask = false
    for (i in remainders.indices) {
      val stack = input.getStackInSlot(i)
      if (!stack.isOf(MOD_ITEMS.potionFlaskItem)) {
        continue
      }
      if (!hasConsumedFlask) {
        hasConsumedFlask = true
        totalLevel = max(totalLevel - FLASK_MAX_USES, 0)
      } else {
        remainders[i] =
            if (totalLevel >= FLASK_MIN_USES) {
              val newStack = stack.copy()
              val newLevel = min(totalLevel, FLASK_MAX_USES)
              newStack.set(MOD_COMPONENTS.flaskRemainingUsesComponent, newLevel)
              totalLevel -= newLevel
              newStack
            } else {
              MOD_ITEMS.glassFlaskItem.defaultStack
            }
      }
    }
    return remainders
  }

  override fun fits(width: Int, height: Int) = width * height >= 2

  override fun getResult(registriesLookup: WrapperLookup) = ItemStack(MOD_ITEMS.potionFlaskItem)

  override fun getSerializer() = MOD_RECIPE_SERIALIZERS.flaskCombineRecipeSerializer
}
