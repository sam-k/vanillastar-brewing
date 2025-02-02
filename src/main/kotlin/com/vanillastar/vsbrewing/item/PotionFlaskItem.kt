package com.vanillastar.vsbrewing.item

import com.vanillastar.vsbrewing.component.MOD_COMPONENTS
import com.vanillastar.vsbrewing.utils.getModIdentifier
import net.minecraft.advancement.criterion.Criteria
import net.minecraft.block.Blocks
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.*
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.particle.ParticleTypes
import net.minecraft.potion.Potions
import net.minecraft.registry.tag.BlockTags
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.stat.Stats
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Util
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import net.minecraft.world.event.GameEvent

val POTION_FLASK_ITEM_MAX_USES = 3

val POTION_FLASK_ITEM_METADATA =
    ModItemMetadata("potion_flask", ItemGroups.INGREDIENTS) {
      it.maxCount(1)
          .component(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT)
          .component(MOD_COMPONENTS.potionFlaskRemainingUsesComponent, POTION_FLASK_ITEM_MAX_USES)
    }

class PotionFlaskItem(settings: Settings) : PotionItem(settings) {
  private val remainingUsesTranslationKey =
      "${Util.createTranslationKey("item", getModIdentifier(POTION_FLASK_ITEM_METADATA.name))}.remaining_uses"

  override fun getDefaultStack(): ItemStack {
    val stack = super.getDefaultStack()
    stack.set(MOD_COMPONENTS.potionFlaskRemainingUsesComponent, 3)
    return stack
  }

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
                  .applyInstantEffect(player, player, user, it.amplifier, /* proximity= */ 1.0)
            } else {
              user.addStatusEffect(it)
            }
          }
    }

    player?.incrementStat(Stats.USED.getOrCreateStat(this))
    val remainingUses = stack.getOrDefault(MOD_COMPONENTS.potionFlaskRemainingUsesComponent, 0)
    if (player?.isInCreativeMode != true) {
      if (remainingUses > 1) {
        stack.set(MOD_COMPONENTS.potionFlaskRemainingUsesComponent, remainingUses - 1)
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

  override fun useOnBlock(context: ItemUsageContext): ActionResult {
    val world = context.world
    val blockPos = context.blockPos
    val player = context.player
    val stack = context.stack

    if (
        context.side == Direction.DOWN ||
            !world.getBlockState(blockPos).isIn(BlockTags.CONVERTABLE_TO_MUD) ||
            !stack
                .getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT)
                .matches(Potions.WATER)
    ) {
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
    val remainingUses = stack.getOrDefault(MOD_COMPONENTS.potionFlaskRemainingUsesComponent, 0)
    if (remainingUses > 1) {
      stack.set(MOD_COMPONENTS.potionFlaskRemainingUsesComponent, remainingUses - 1)
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

  override fun appendTooltip(
      stack: ItemStack,
      context: TooltipContext,
      tooltip: MutableList<Text>,
      type: TooltipType,
  ) {
    super.appendTooltip(stack, context, tooltip, type)
    tooltip.add(
        Text.translatable(
                remainingUsesTranslationKey,
                stack.get(MOD_COMPONENTS.potionFlaskRemainingUsesComponent),
            )
            .formatted(Formatting.GRAY)
    )
  }
}
