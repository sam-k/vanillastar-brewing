package com.vanillastar.vsbrewing.item

import com.vanillastar.vsbrewing.block.FlaskBlock
import com.vanillastar.vsbrewing.block.FlaskBlock.Companion.WATERLOGGED
import com.vanillastar.vsbrewing.block.MOD_BLOCKS
import com.vanillastar.vsbrewing.component.MOD_COMPONENTS
import kotlin.String
import kotlin.repeat
import kotlin.streams.asStream
import net.minecraft.advancement.criterion.Criteria
import net.minecraft.block.Blocks
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.fluid.Fluids
import net.minecraft.item.AliasedBlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemGroups
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsage
import net.minecraft.item.ItemUsageContext
import net.minecraft.item.PotionItem
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.particle.ParticleTypes
import net.minecraft.potion.Potion
import net.minecraft.potion.Potions
import net.minecraft.registry.Registries
import net.minecraft.registry.tag.BlockTags
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.stat.Stats
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.util.UseAction
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import net.minecraft.world.event.GameEvent

val POTION_FLASK_ITEM_METADATA_IN_CONTEXT: ModItemMetadataInContext = {
  ModItemMetadata(
      "potion_flask",
      ItemGroups.FOOD_AND_DRINK,
      /* previousItem= */ null,
      Registries.POTION.streamEntries()
          .flatMap { potion ->
            (PotionFlaskItem.MAX_USES downTo PotionFlaskItem.MIN_USES)
                .asSequence()
                .map { remainingUses ->
                  ModItemGroupVisibilityMetadata(
                      if (remainingUses < PotionFlaskItem.MAX_USES) {
                        ItemGroup.StackVisibility.SEARCH_TAB_ONLY
                      } else {
                        ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS
                      },
                      getPotionFlaskStackProvider(potion) { stack ->
                        stack.set(MOD_COMPONENTS.flaskRemainingUsesComponent, remainingUses)
                      },
                  )
                }
                .asStream()
          }
          .toList(),
  ) {
    it.maxCount(1)
        .component(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT)
        .component(MOD_COMPONENTS.flaskRemainingUsesComponent, PotionFlaskItem.MAX_USES)
        .recipeRemainder(this.glassFlaskItem)
  }
}

/** [Item] for a potion-filled flask. */
class PotionFlaskItem(settings: Settings) : AliasedBlockItem(MOD_BLOCKS.flaskBlock, settings) {
  companion object {
    /** Minimum number of uses for a flask item. */
    const val MIN_USES = 1

    /** Maximum number of uses for a flask item. */
    const val MAX_USES = 3

    /** Duration for drinking a flask item, in ticks. */
    const val MAX_USE_TIME = 32
  }

  override fun getDefaultStack(): ItemStack {
    val stack = super.getDefaultStack()
    stack.set(MOD_COMPONENTS.flaskRemainingUsesComponent, MAX_USES)
    stack.set(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent(Potions.WATER))
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
  override fun finishUsing(stack: ItemStack, world: World, user: LivingEntity): ItemStack {
    val player = user as? PlayerEntity
    if (player is ServerPlayerEntity) {
      Criteria.CONSUME_ITEM.trigger(player, stack)
    }

    if (!world.isClient) {
      stack
          .getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT)
          .forEachEffect {
            if (it.effectType.value().isInstant) {
              it.effectType
                  .value()
                  .applyInstantEffect(user, user, user, it.amplifier, /* proximity= */ 1.0)
            } else {
              user.addStatusEffect(it)
            }
          }
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

  /**
   * Uses the flask on a block.
   *
   * This tries to place the flask as a block first, before trying to apply the flask on a block as
   * an item.
   */
  override fun useOnBlock(context: ItemUsageContext): ActionResult {
    val player = context.player
    if (player != null && player.isSneaking) {
      // Place only if sneaking, so as not to interfere with drinking the potion.
      return this.place(ItemPlacementContext(context))
    }

    val world = context.world
    val blockPos = context.blockPos
    val stack = context.stack
    if (
        context.side == Direction.DOWN ||
            !world.getBlockState(blockPos).isIn(BlockTags.CONVERTABLE_TO_MUD) ||
            !stack
                .getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT)
                .matches(Potions.WATER)
    ) {
      // Do nothing, as conversion to mud is inapplicable here.
      return ActionResult.PASS
    }

    world.playSound(
        /* source= */ null,
        blockPos,
        SoundEvents.ENTITY_GENERIC_SPLASH,
        SoundCategory.BLOCKS,
        /* volume= */ 1.0f,
        /* pitch= */ 1.0f,
    )

    player?.incrementStat(Stats.USED.getOrCreateStat(stack.item))

    // If applicable, decrement remaining uses instead of discarding item.
    val remainingUses = stack.getOrDefault(MOD_COMPONENTS.flaskRemainingUsesComponent, 0)
    if (remainingUses > MIN_USES && player?.isInCreativeMode != true) {
      stack.set(MOD_COMPONENTS.flaskRemainingUsesComponent, remainingUses - 1)
    } else {
      player?.setStackInHand(
          context.hand,
          ItemUsage.exchangeStack(stack, player, ItemStack(MOD_ITEMS.glassFlaskItem)),
      )
    }

    if (!world.isClient) {
      val serverWorld = world as ServerWorld
      repeat(5) {
        serverWorld.spawnParticles(
            ParticleTypes.SPLASH,
            blockPos.x + world.random.nextDouble(),
            blockPos.y + 1.0,
            blockPos.z + world.random.nextDouble(),
            /* count= */ 1,
            /* deltaX= */ 0.0,
            /* deltaY= */ 0.0,
            /* deltaZ= */ 0.0,
            /* speed= */ 1.0,
        )
      }
    }

    world.playSound(
        /* source= */ null,
        blockPos,
        SoundEvents.ITEM_BOTTLE_EMPTY,
        SoundCategory.BLOCKS,
        /* volume= */ 1.0f,
        /* pitch= */ 1.0f,
    )
    world.emitGameEvent(/* entity= */ null, GameEvent.FLUID_PLACE, blockPos)
    world.setBlockState(blockPos, Blocks.MUD.defaultState)
    return ActionResult.success(world.isClient)
  }

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

  /** This is copied from [PotionItem.getTranslationKey]. */
  override fun getTranslationKey(stack: ItemStack): String =
      Potion.finishTranslationKey(
          stack
              .getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT)
              .potion(),
          this.translationKey + ".effect.",
      )

  override fun appendTooltip(
      stack: ItemStack,
      context: TooltipContext,
      tooltip: MutableList<Text>,
      type: TooltipType,
  ) {
    appendPotionFlaskDataTooltip(stack, context, tooltip)
  }
}
