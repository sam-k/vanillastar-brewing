package com.vanillastar.vsbrewing.item

import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemGroups
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsage
import net.minecraft.item.Items
import net.minecraft.potion.Potions
import net.minecraft.registry.tag.FluidTags
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.stat.Stats
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.util.hit.HitResult
import net.minecraft.world.RaycastContext
import net.minecraft.world.World
import net.minecraft.world.event.GameEvent

val GLASS_FLASK_ITEM_METADATA =
    ModItemMetadata("glass_flask", ItemGroups.INGREDIENTS, /* previousItem= */ Items.GLASS_BOTTLE) {
      it.maxCount(64)
    }

class GlassFlaskItem(settings: Settings) : Item(settings) {
  override fun use(world: World, player: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
    val stack = player.getStackInHand(hand)

    val blockHitResult = raycast(world, player, RaycastContext.FluidHandling.SOURCE_ONLY)
    if (blockHitResult.type != HitResult.Type.BLOCK) {
      return TypedActionResult.pass(stack)
    }

    val blockPos = blockHitResult.blockPos
    if (
        !world.canPlayerModifyAt(player, blockPos) ||
            !world.getFluidState(blockPos).isIn(FluidTags.WATER)
    ) {
      return TypedActionResult.pass(stack)
    }

    world.playSound(
        player,
        player.x,
        player.y,
        player.z,
        SoundEvents.ITEM_BOTTLE_FILL,
        SoundCategory.NEUTRAL,
        /* volume= */ 1.0f,
        /* pitch= */ 1.0f,
    )
    world.emitGameEvent(player, GameEvent.FLUID_PICKUP, blockPos)
    player.incrementStat(Stats.USED.getOrCreateStat(this))
    return TypedActionResult.success(
        ItemUsage.exchangeStack(
            stack,
            player,
            PotionContentsComponent.createStack(MOD_ITEMS.potionFlaskItem, Potions.WATER),
        ),
        world.isClient(),
    )
  }
}
