package com.vanillastar.vsbrewing.block

import com.vanillastar.vsbrewing.block.entity.MOD_BLOCK_ENTITIES
import com.vanillastar.vsbrewing.block.entity.PotionCauldronBlockEntity
import com.vanillastar.vsbrewing.component.MOD_COMPONENTS
import com.vanillastar.vsbrewing.item.DrinkableFlaskItem
import com.vanillastar.vsbrewing.item.MOD_ITEMS
import com.vanillastar.vsbrewing.utils.ModRegistry
import kotlin.jvm.optionals.getOrDefault
import kotlin.jvm.optionals.getOrNull
import kotlin.math.min
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.LeveledCauldronBlock
import net.minecraft.block.cauldron.CauldronBehavior
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsage
import net.minecraft.item.Items
import net.minecraft.potion.Potions
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.stat.Stats
import net.minecraft.util.Hand
import net.minecraft.util.ItemActionResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.world.event.GameEvent

abstract class ModCauldronBehaviors : ModRegistry() {
  @JvmField
  val potionCauldronBehavior: CauldronBehavior.CauldronBehaviorMap =
      CauldronBehavior.createMap("potion")

  override fun initialize() {
    val emptyCauldronBehaviorMap = CauldronBehavior.EMPTY_CAULDRON_BEHAVIOR.map()
    emptyCauldronBehaviorMap.put(Items.POTION, fillCauldronBehavior) // Replace
    emptyCauldronBehaviorMap.put(MOD_ITEMS.potionFlaskItem, fillCauldronBehavior)

    val waterCauldronBehaviorMap = CauldronBehavior.WATER_CAULDRON_BEHAVIOR.map()
    waterCauldronBehaviorMap.put(MOD_ITEMS.glassFlaskItem, takeFromCauldronBehavior)

    val potionCauldronBehaviorMap = this.potionCauldronBehavior.map()
    CauldronBehavior.registerBucketBehavior(potionCauldronBehaviorMap)
    potionCauldronBehaviorMap.put(Items.POTION, fillCauldronBehavior)
    potionCauldronBehaviorMap.put(MOD_ITEMS.potionFlaskItem, fillCauldronBehavior)
    potionCauldronBehaviorMap.put(Items.GLASS_BOTTLE, takeFromCauldronBehavior)
    potionCauldronBehaviorMap.put(MOD_ITEMS.glassFlaskItem, takeFromCauldronBehavior)
  }

  private companion object {
    fun getCauldronPotionContents(
        state: BlockState,
        world: World,
        pos: BlockPos,
    ): PotionContentsComponent? =
        when {
          state.isOf(Blocks.WATER_CAULDRON) -> PotionContentsComponent(Potions.WATER)
          state.isOf(MOD_BLOCKS.potionCauldronBlock) ->
              world
                  .getBlockEntity(pos, MOD_BLOCK_ENTITIES.potionCauldronBlockEntityType)
                  .getOrNull()
                  ?.potionContents
          else -> null
        }

    fun updateChanges(state: BlockState, newState: BlockState, world: World, pos: BlockPos) {
      world
          .getBlockEntity(pos, MOD_BLOCK_ENTITIES.potionCauldronBlockEntityType)
          .getOrNull()
          ?.markDirty()
      world.updateListeners(pos, state, newState, /* flags= */ 0)
    }

    val fillCauldronBehavior =
        CauldronBehavior {
            state: BlockState,
            world: World,
            pos: BlockPos,
            player: PlayerEntity,
            hand: Hand,
            stack: ItemStack ->
          val stackPotionContents = stack.get(DataComponentTypes.POTION_CONTENTS)
          if (stackPotionContents == null) {
            return@CauldronBehavior ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
          }

          val cauldronLevel = state.getOrEmpty(LeveledCauldronBlock.LEVEL).getOrDefault(0)
          if (cauldronLevel == LeveledCauldronBlock.MAX_LEVEL) {
            return@CauldronBehavior ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
          }

          val cauldronPotionContents = getCauldronPotionContents(state, world, pos)
          if (cauldronPotionContents != null && stackPotionContents != cauldronPotionContents) {
            return@CauldronBehavior ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
          }

          val (diffLevel, remainderStack) =
              when {
                stack.isOf(Items.POTION) -> Pair(/* diffLevel= */ 1, ItemStack(Items.GLASS_BOTTLE))

                stack.isOf(MOD_ITEMS.potionFlaskItem) -> {
                  val remainingUses =
                      stack.getOrDefault(
                          MOD_COMPONENTS.flaskRemainingUsesComponent,
                          DrinkableFlaskItem.MAX_USES,
                      )
                  val diffLevel = min(remainingUses, LeveledCauldronBlock.MAX_LEVEL - cauldronLevel)
                  if (remainingUses - diffLevel > 0) {
                    val newStack = stack.copy()
                    newStack.set(
                        MOD_COMPONENTS.flaskRemainingUsesComponent,
                        remainingUses - diffLevel,
                    )
                    Pair(diffLevel, newStack)
                  } else {
                    Pair(diffLevel, ItemStack(MOD_ITEMS.glassFlaskItem))
                  }
                }

                else -> Pair(/* diffLevel= */ 0, /* remainderStack= */ null)
              }
          if (remainderStack == null) {
            return@CauldronBehavior ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
          }

          if (world.isClient) {
            return@CauldronBehavior ItemActionResult.success(/* swingHand= */ true)
          }

          player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, remainderStack))
          player.incrementStat(Stats.USE_CAULDRON)
          player.incrementStat(Stats.USED.getOrCreateStat(stack.item))

          val newState =
              if (stackPotionContents.matches(Potions.WATER)) {
                Blocks.WATER_CAULDRON.defaultState.with(
                    LeveledCauldronBlock.LEVEL,
                    cauldronLevel + diffLevel,
                )
              } else {
                MOD_BLOCKS.potionCauldronBlock.defaultState.with(
                    LeveledCauldronBlock.LEVEL,
                    cauldronLevel + diffLevel,
                )
              }
          world.setBlockState(pos, newState)
          if (newState.isOf(MOD_BLOCKS.potionCauldronBlock)) {
            world.addBlockEntity(PotionCauldronBlockEntity(pos, newState, stackPotionContents))
          }
          updateChanges(state, newState, world, pos)
          world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(newState))
          world.playSound(
              /* source= */ null,
              pos,
              SoundEvents.ITEM_BOTTLE_EMPTY,
              SoundCategory.BLOCKS,
              /* volume= */ 1.0f,
              /* pitch= */ 1.0f,
          )
          world.emitGameEvent(/* entity= */ null, GameEvent.FLUID_PLACE, pos)

          ItemActionResult.success(/* swingHand= */ false)
        }

    val takeFromCauldronBehavior =
        CauldronBehavior {
            state: BlockState,
            world: World,
            pos: BlockPos,
            player: PlayerEntity,
            hand: Hand,
            stack: ItemStack ->
          val cauldronPotionContents = getCauldronPotionContents(state, world, pos)
          if (cauldronPotionContents == null) {
            return@CauldronBehavior ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
          }

          val cauldronLevel = state.getOrEmpty(LeveledCauldronBlock.LEVEL).getOrDefault(0)
          val (diffLevel, potionStack) =
              when {
                stack.isOf(Items.GLASS_BOTTLE) -> {
                  val newStack = ItemStack(Items.POTION)
                  newStack.set(DataComponentTypes.POTION_CONTENTS, cauldronPotionContents)
                  Pair(/* diffLevel= */ 1, newStack)
                }

                stack.isOf(MOD_ITEMS.glassFlaskItem) -> {
                  val newStack = ItemStack(MOD_ITEMS.potionFlaskItem)
                  newStack.set(DataComponentTypes.POTION_CONTENTS, cauldronPotionContents)
                  newStack.set(MOD_COMPONENTS.flaskRemainingUsesComponent, cauldronLevel)
                  Pair(cauldronLevel, newStack)
                }

                else -> Pair(/* diffLevel= */ 0, /* potionStack= */ null)
              }
          if (potionStack == null) {
            return@CauldronBehavior ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
          }

          if (world.isClient) {
            return@CauldronBehavior ItemActionResult.success(world.isClient)
          }

          player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, potionStack))
          player.incrementStat(Stats.USE_CAULDRON)
          player.incrementStat(Stats.USED.getOrCreateStat(stack.item))

          val newState =
              if (cauldronLevel - diffLevel < LeveledCauldronBlock.MIN_LEVEL) {
                Blocks.CAULDRON.defaultState
              } else {
                state.with(LeveledCauldronBlock.LEVEL, cauldronLevel - diffLevel)
              }
          world.setBlockState(pos, newState)
          updateChanges(state, newState, world, pos)
          world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(newState))
          world.playSound(
              /* source= */ null,
              pos,
              SoundEvents.ITEM_BOTTLE_FILL,
              SoundCategory.BLOCKS,
              /* volume= */ 1.0f,
              /* pitch= */ 1.0f,
          )
          world.emitGameEvent(/* entity= */ null, GameEvent.FLUID_PICKUP, pos)

          ItemActionResult.success(world.isClient)
        }
  }
}

@JvmField val MOD_CAULDRON_BEHAVIORS = object : ModCauldronBehaviors() {}
