package com.vanillastar.vsbrewing.item

import com.vanillastar.vsbrewing.component.MOD_COMPONENTS
import net.minecraft.entity.LivingEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemGroups
import net.minecraft.item.ItemStack
import net.minecraft.world.World

val MILK_FLASK_ITEM_METADATA_IN_CONTEXT: ModItemMetadataInContext = {
  ModItemMetadata(
      "milk_flask",
      ItemGroups.FOOD_AND_DRINK,
      /* previousItem= */ this.milkBottleItem,
      (DrinkableFlaskItem.MAX_USES downTo DrinkableFlaskItem.MIN_USES).map { remainingUses ->
        ModItemGroupVisibilityMetadata(
            if (remainingUses < DrinkableFlaskItem.MAX_USES) {
              ItemGroup.StackVisibility.SEARCH_TAB_ONLY
            } else {
              ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS
            }
        ) { stack ->
          stack.set(MOD_COMPONENTS.flaskRemainingUsesComponent, remainingUses)
        }
      },
  ) {
    it.maxCount(1)
        .component(MOD_COMPONENTS.flaskRemainingUsesComponent, DrinkableFlaskItem.MAX_USES)
        .recipeRemainder(this.glassFlaskItem)
  }
}

/** [Item] for a milk-filled flask. */
class MilkFlaskItem(settings: Settings) : DrinkableFlaskItem(settings) {
  override fun onFinishUsing(stack: ItemStack, world: World, user: LivingEntity) {
    user.clearStatusEffects()
  }
}
