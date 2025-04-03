package com.vanillastar.vsbrewing.recipe

import com.vanillastar.vsbrewing.item.MOD_ITEMS
import net.minecraft.item.ItemStack
import net.minecraft.recipe.SpecialCraftingRecipe
import net.minecraft.recipe.book.CraftingRecipeCategory
import net.minecraft.registry.RegistryWrapper.WrapperLookup

abstract class AbstractFlaskRecipe(category: CraftingRecipeCategory) :
    SpecialCraftingRecipe(category) {
  override fun fits(width: Int, height: Int) = width * height >= 2

  override fun getResult(registriesLookup: WrapperLookup) = ItemStack(MOD_ITEMS.potionFlaskItem)
}
