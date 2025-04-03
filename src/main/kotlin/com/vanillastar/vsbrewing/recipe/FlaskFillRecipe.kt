package com.vanillastar.vsbrewing.recipe

import com.vanillastar.vsbrewing.component.MOD_COMPONENTS
import com.vanillastar.vsbrewing.item.FLASK_MAX_USES
import com.vanillastar.vsbrewing.item.FLASK_MIN_USES
import com.vanillastar.vsbrewing.item.GlassFlaskItem
import com.vanillastar.vsbrewing.item.MOD_ITEMS
import com.vanillastar.vsbrewing.item.PotionFlaskItem
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.item.PotionItem
import net.minecraft.potion.Potion
import net.minecraft.recipe.SpecialCraftingRecipe
import net.minecraft.recipe.book.CraftingRecipeCategory
import net.minecraft.recipe.input.CraftingRecipeInput
import net.minecraft.registry.RegistryWrapper.WrapperLookup
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.util.collection.DefaultedList
import net.minecraft.world.World

/**
 * [SpecialCraftingRecipe] for filling a [GlassFlaskItem] or [PotionFlaskItem] with up to three
 * matching [PotionItem]'s, depending on the existing flask level.
 */
class FlaskFillRecipe(category: CraftingRecipeCategory) : AbstractFlaskRecipe(category) {
  private companion object {
    /** Gets the output flask's new level and [Potion] entry, or `null` if the recipe is invalid. */
    fun getCraftingResults(input: CraftingRecipeInput): Pair<Int, RegistryEntry<Potion>>? {
      var flaskStack: ItemStack? = null
      val potionStacks = mutableListOf<ItemStack>()
      for (stack in input.stacks) {
        if (stack.isOf(MOD_ITEMS.glassFlaskItem) || stack.isOf(MOD_ITEMS.potionFlaskItem)) {
          if (flaskStack != null) {
            return null
          }
          flaskStack = stack
        } else if (stack.isOf(Items.POTION)) {
          potionStacks.add(stack)
        } else if (!stack.isEmpty) {
          return null
        }
      }
      if (flaskStack == null || potionStacks.isEmpty()) {
        return null
      }

      val newLevel =
          flaskStack.getOrDefault(MOD_COMPONENTS.flaskRemainingUsesComponent, 0) + potionStacks.size
      if (
          newLevel < FLASK_MIN_USES ||
              newLevel > FLASK_MAX_USES ||
              potionStacks.any { !it.contains(DataComponentTypes.POTION_CONTENTS) }
      ) {
        return null
      }

      val referencePotionEntry =
          if (flaskStack.contains(DataComponentTypes.POTION_CONTENTS)) {
                flaskStack
              } else {
                potionStacks.removeFirst()
              }
              .get(DataComponentTypes.POTION_CONTENTS)
              ?.potion
              ?.orElse(null)
      if (
          referencePotionEntry == null ||
              potionStacks.any {
                it.get(DataComponentTypes.POTION_CONTENTS)?.matches(referencePotionEntry) != true
              }
      ) {
        return null
      }

      return Pair(newLevel, referencePotionEntry)
    }
  }

  override fun matches(input: CraftingRecipeInput, world: World) = getCraftingResults(input) != null

  override fun craft(input: CraftingRecipeInput, lookup: WrapperLookup): ItemStack {
    val parts = getCraftingResults(input)
    if (parts == null) {
      return ItemStack.EMPTY
    }

    val (newLevel, potionEntry) = parts
    val newStack = PotionContentsComponent.createStack(MOD_ITEMS.potionFlaskItem, potionEntry)
    newStack.set(MOD_COMPONENTS.flaskRemainingUsesComponent, newLevel)
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

  override fun getSerializer() = MOD_RECIPE_SERIALIZERS.flaskFillRecipeSerializer
}
