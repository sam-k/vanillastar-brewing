package com.vanillastar.vsbrewing.recipe

import com.vanillastar.vsbrewing.component.MOD_COMPONENTS
import com.vanillastar.vsbrewing.item.FLASK_MIN_USES
import com.vanillastar.vsbrewing.item.MOD_ITEMS
import com.vanillastar.vsbrewing.item.PotionFlaskItem
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.item.GlassBottleItem
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
 * [SpecialCraftingRecipe] for emptying one level of a [PotionFlaskItem] into a [GlassBottleItem].
 */
class FlaskEmptyRecipe(category: CraftingRecipeCategory) : AbstractFlaskRecipe(category) {
  private companion object {
    /** Gets the input flask's new level and [Potion] entry, or `null` if the recipe is invalid. */
    fun getCraftingResults(input: CraftingRecipeInput): Pair<Int, RegistryEntry<Potion>>? {
      var flaskStack: ItemStack? = null
      var bottleStack: ItemStack? = null
      for (stack in input.stacks) {
        if (stack.isOf(MOD_ITEMS.potionFlaskItem)) {
          if (flaskStack != null) {
            return null
          }
          flaskStack = stack
        } else if (stack.isOf(Items.GLASS_BOTTLE)) {
          if (bottleStack != null) {
            return null
          }
          bottleStack = stack
        } else if (!stack.isEmpty) {
          return null
        }
      }
      if (flaskStack == null || bottleStack == null) {
        return null
      }

      val potionEntry = flaskStack.get(DataComponentTypes.POTION_CONTENTS)?.potion?.orElse(null)
      if (potionEntry == null) {
        return null
      }

      return Pair(
          flaskStack.getOrDefault(MOD_COMPONENTS.flaskRemainingUsesComponent, 0) - 1,
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
    val (_, potionEntry) = parts
    return PotionContentsComponent.createStack(Items.POTION, potionEntry)
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

  override fun getSerializer() = MOD_RECIPE_SERIALIZERS.flaskEmptyRecipeSerializer
}
