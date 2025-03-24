package com.vanillastar.vsbrewing.item

import com.vanillastar.vsbrewing.component.MOD_COMPONENTS
import net.minecraft.entity.LivingEntity
import net.minecraft.item.*
import net.minecraft.world.World

val MILK_FLASK_ITEM_METADATA =
    ModItemMetadata(
        "milk_flask",
        ItemGroups.FOOD_AND_DRINK,
        /* previousItem= */ Items.MILK_BUCKET,
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
    }

/** [Item] for a milk-filled flask. */
class MilkFlaskItem(settings: Settings) : DrinkableFlaskItem(settings) {
  override fun onFinishUsing(stack: ItemStack, world: World, user: LivingEntity) {
    user.clearStatusEffects()
  }
}
