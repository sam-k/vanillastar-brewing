package com.vanillastar.vsbrewing.item

import com.vanillastar.vsbrewing.block.FlaskBlock
import com.vanillastar.vsbrewing.block.FlaskBlock.Companion.WATERLOGGED
import com.vanillastar.vsbrewing.block.MOD_BLOCKS
import com.vanillastar.vsbrewing.component.MOD_COMPONENTS
import com.vanillastar.vsbrewing.utils.getModIdentifier
import net.minecraft.advancement.criterion.Criteria
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.fluid.Fluids
import net.minecraft.item.AliasedBlockItem
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsage
import net.minecraft.item.ItemUsageContext
import net.minecraft.item.PotionItem
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.stat.Stats
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.util.UseAction
import net.minecraft.util.Util
import net.minecraft.world.World
import net.minecraft.world.event.GameEvent

abstract class DrinkableFlaskItem(settings: Settings) :
    AliasedBlockItem(MOD_BLOCKS.flaskBlock, settings) {
  companion object {
    /** Minimum number of uses for a flask item. */
    const val MIN_USES = 1

    /** Maximum number of uses for a flask item. */
    const val MAX_USES = 3

    /** Duration for drinking a flask item, in ticks. */
    const val MAX_USE_TIME = 32

    /** Builds and appends the item tooltip displaying the remaining uses of this potion flask. */
    fun appendRemainingUsesTooltip(stack: ItemStack, tooltip: MutableList<Text>) {
      val translationKeyPrefix =
          Util.createTranslationKey("item", getModIdentifier(GLASS_FLASK_ITEM_METADATA.name))
      tooltip.add(
          Text.translatable(
                  "${translationKeyPrefix}.remaining_uses",
                  stack.get(MOD_COMPONENTS.flaskRemainingUsesComponent),
              )
              .formatted(Formatting.GRAY)
      )
    }
  }

  override fun getDefaultStack(): ItemStack {
    val stack = super.getDefaultStack()
    stack.set(MOD_COMPONENTS.flaskRemainingUsesComponent, MAX_USES)
    return stack
  }

  override fun getMaxUseTime(stack: ItemStack, user: LivingEntity) = MAX_USE_TIME

  override fun getUseAction(stack: ItemStack) = UseAction.DRINK

  override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> =
      ItemUsage.consumeHeldItem(world, user, hand)

  /**
   * This is mostly copied from [PotionItem.finishUsing], except we try to decrement the flask's
   * remaining uses instead of immediately emptying the flask.
   */
  final override fun finishUsing(stack: ItemStack, world: World, user: LivingEntity): ItemStack {
    val player = user as? PlayerEntity
    if (player is ServerPlayerEntity) {
      Criteria.CONSUME_ITEM.trigger(player, stack)
    }

    if (!world.isClient) {
      this.onFinishUsing(stack, world, user)
    }

    player?.incrementStat(Stats.USED.getOrCreateStat(this))

    if (player?.isInCreativeMode != true) {
      // If applicable, decrement remaining uses instead of discarding item.
      val remainingUses = stack.getOrDefault(MOD_COMPONENTS.flaskRemainingUsesComponent, MAX_USES)
      if (remainingUses > MIN_USES) {
        stack.set(MOD_COMPONENTS.flaskRemainingUsesComponent, remainingUses - 1)
      } else {
        stack.decrementUnlessCreative(/* amount= */ 1, player)
      }
      if (stack.isEmpty) {
        return ItemStack(MOD_ITEMS.glassFlaskItem)
      }
    }

    user.emitGameEvent(GameEvent.DRINK)
    return stack
  }

  /** Action to perform upon using the flask. */
  open fun onFinishUsing(stack: ItemStack, world: World, user: LivingEntity) {}

  /**
   * Uses the flask on a block.
   *
   * This tries to place the flask as a block first, before trying to apply the flask on a block as
   * an item.
   */
  final override fun useOnBlock(context: ItemUsageContext): ActionResult {
    val player = context.player
    if (player != null && player.isSneaking) {
      // Place only if sneaking, so as not to interfere with drinking the potion.
      return this.place(ItemPlacementContext(context))
    }
    return this.useOnBlockAsItem(context)
  }

  /** Action to perform upon using the flask on a block as an item. */
  open fun useOnBlockAsItem(context: ItemUsageContext) = ActionResult.PASS

  override fun getPlacementState(context: ItemPlacementContext) =
      super.getPlacementState(context)
          ?.with(
              FlaskBlock.LEVEL,
              context.stack.getOrDefault(MOD_COMPONENTS.flaskRemainingUsesComponent, 0),
          )
          ?.with(
              WATERLOGGED,
              context.world.getFluidState(context.blockPos).fluid.matchesType(Fluids.WATER),
          )

  override fun appendTooltip(
      stack: ItemStack,
      context: TooltipContext,
      tooltip: MutableList<Text>,
      type: TooltipType,
  ) {
    appendRemainingUsesTooltip(stack, tooltip)
  }
}
