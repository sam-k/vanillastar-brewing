package com.vanillastar.vsbrewing.item

import com.vanillastar.vsbrewing.block.BottleBlock.Companion.WATERLOGGED
import com.vanillastar.vsbrewing.block.MOD_BLOCKS
import net.minecraft.advancement.criterion.Criteria
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.fluid.Fluids
import net.minecraft.item.AliasedBlockItem
import net.minecraft.item.ItemGroups
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsage
import net.minecraft.item.ItemUsageContext
import net.minecraft.item.Items
import net.minecraft.item.PotionItem
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.stat.Stats
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.util.UseAction
import net.minecraft.world.World
import net.minecraft.world.event.GameEvent

val MILK_BOTTLE_ITEM_METADATA =
    ModItemMetadata(
        "milk_bottle",
        ItemGroups.FOOD_AND_DRINK,
        /* previousItem= */ Items.MILK_BUCKET,
    ) {
      it.maxCount(1)
    }

class MilkBottleItem(settings: Settings) : AliasedBlockItem(MOD_BLOCKS.bottleBlock, settings) {
  companion object {
    /** Duration for drinking a milk bottle, in ticks. */
    const val MAX_USE_TIME = 32
  }

  override fun getMaxUseTime(stack: ItemStack, user: LivingEntity) = MAX_USE_TIME

  override fun getUseAction(stack: ItemStack) = UseAction.DRINK

  override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> =
      ItemUsage.consumeHeldItem(world, user, hand)

  /** This is mostly copied from [PotionItem.finishUsing]. */
  override fun finishUsing(stack: ItemStack, world: World, user: LivingEntity): ItemStack {
    val player = user as? PlayerEntity
    if (player is ServerPlayerEntity) {
      Criteria.CONSUME_ITEM.trigger(player, stack)
    }

    if (!world.isClient) {
      user.clearStatusEffects()
    }

    if (player != null) {
      player.incrementStat(Stats.USED.getOrCreateStat(this))
      stack.decrementUnlessCreative(1, player)
    }

    if (player?.isInCreativeMode != true) {
      if (stack.isEmpty) {
        return ItemStack(Items.GLASS_BOTTLE)
      }
      player?.getInventory()?.insertStack(ItemStack(Items.GLASS_BOTTLE))
    }

    user.emitGameEvent(GameEvent.DRINK)
    return stack
  }

  override fun useOnBlock(context: ItemUsageContext): ActionResult {
    val player = context.player
    if (player != null && player.isSneaking) {
      // Place only if sneaking, so as not to interfere with drinking the potion.
      return this.place(ItemPlacementContext(context))
    }
    return ActionResult.PASS
  }

  override fun getPlacementState(context: ItemPlacementContext) =
      super.getPlacementState(context)
          ?.with(
              WATERLOGGED,
              context.world.getFluidState(context.blockPos).fluid.matchesType(Fluids.WATER),
          )
}
