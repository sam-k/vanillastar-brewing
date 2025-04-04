package com.vanillastar.vsbrewing.recipe

import com.vanillastar.vsbrewing.utils.ModRegistry
import com.vanillastar.vsbrewing.utils.getModIdentifier
import net.minecraft.recipe.CraftingRecipe
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.SpecialRecipeSerializer
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry

abstract class ModRecipeSerializers() : ModRegistry() {
  @JvmField
  val flaskFillRecipeSerializer =
      this.registerSpecialRecipeSerializer("flaskfill", ::FlaskFillRecipe)

  @JvmField
  val flaskCombineRecipeSerializer =
      this.registerSpecialRecipeSerializer("flaskcombine", ::FlaskCombineRecipe)

  @JvmField
  val flaskEmptyRecipeSerializer =
      this.registerSpecialRecipeSerializer("flaskempty", ::FlaskEmptyRecipe)

  private fun <TRecipe : CraftingRecipe> registerSpecialRecipeSerializer(
      name: String,
      constructor: SpecialRecipeSerializer.Factory<TRecipe>,
  ): RecipeSerializer<TRecipe> {
    val id = getModIdentifier("crafting_special_${name}")
    val recipeSerializer =
        Registry.register(Registries.RECIPE_SERIALIZER, id, SpecialRecipeSerializer(constructor))
    this.logger.info("Registered recipe serializer {}", id)
    return recipeSerializer
  }

  override fun initialize() {}
}

@JvmField val MOD_RECIPE_SERIALIZERS = object : ModRecipeSerializers() {}
