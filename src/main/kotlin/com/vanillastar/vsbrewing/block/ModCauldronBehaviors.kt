package com.vanillastar.vsbrewing.block

import com.vanillastar.vsbrewing.block.entity.PotionCauldronBlockEntity
import com.vanillastar.vsbrewing.component.MOD_COMPONENTS
import com.vanillastar.vsbrewing.item.MOD_ITEMS
import com.vanillastar.vsbrewing.item.PotionFlaskItem
import com.vanillastar.vsbrewing.potion.MILK_POTION_ID
import com.vanillastar.vsbrewing.potion.potionContentsMatchId
import com.vanillastar.vsbrewing.utils.ModRegistry
import kotlin.math.min
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.LeveledCauldronBlock
import net.minecraft.block.cauldron.CauldronBehavior
import net.minecraft.block.entity.BlockEntity
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
    // We register only those behaviors that are not already registered in vanilla.

    val emptyCauldronBehaviorMap = CauldronBehavior.EMPTY_CAULDRON_BEHAVIOR.map()
    emptyCauldronBehaviorMap.putAll(
        listOf(Items.MILK_BUCKET, Items.POTION, MOD_ITEMS.potionFlaskItem).map {
          it to fillCauldronBehavior
        }
    )

    val waterCauldronBehaviorMap = CauldronBehavior.WATER_CAULDRON_BEHAVIOR.map()
    waterCauldronBehaviorMap.put(MOD_ITEMS.glassFlaskItem, takeFromCauldronBehavior)

    val potionCauldronBehaviorMap = this.potionCauldronBehavior.map()
    potionCauldronBehaviorMap.putAll(
        listOf(Items.MILK_BUCKET, Items.POTION, MOD_ITEMS.potionFlaskItem).map {
          it to fillCauldronBehavior
        }
    )
    potionCauldronBehaviorMap.putAll(
        listOf(Items.BUCKET, Items.GLASS_BOTTLE, MOD_ITEMS.glassFlaskItem).map {
          it to takeFromCauldronBehavior
        }
    )
  }

  private enum class ItemType {
    BOTTLE,
    BUCKET,
    FLASK,
  }

  private enum class ContentType {
    EMPTY,
    MILK,
    POTION,
    WATER,
  }

  private companion object {
    val fillCauldronBehavior =
        buildCauldronActionBehavior(shouldFill = true) { stack, state, blockEntity ->
          val cauldronLevel = state.getOrEmpty(LeveledCauldronBlock.LEVEL).orElse(0)
          if (cauldronLevel >= LeveledCauldronBlock.MAX_LEVEL) {
            // This cauldron cannot be filled any further.
            return@buildCauldronActionBehavior Pair(/* newState= */ null, /* newStack= */ null)
          }

          val stackItemType = getItemType(stack)
          val stackContentType = getItemContentType(stack)
          val flaskRemainingUses = stack.get(MOD_COMPONENTS.flaskRemainingUsesComponent) ?: 0

          val diffLevel =
              when (stackItemType) {
                ItemType.BOTTLE -> 1
                ItemType.BUCKET -> LeveledCauldronBlock.MAX_LEVEL - cauldronLevel
                ItemType.FLASK ->
                    min(flaskRemainingUses, LeveledCauldronBlock.MAX_LEVEL - cauldronLevel)
                else -> 0
              }
          if (diffLevel <= 0) {
            return@buildCauldronActionBehavior Pair(/* newState= */ null, /* newStack= */ null)
          }

          val newStack =
              when (stackItemType) {
                ItemType.BOTTLE,
                ItemType.BUCKET -> stack.recipeRemainder
                ItemType.FLASK -> {
                  if (flaskRemainingUses - diffLevel >= PotionFlaskItem.MIN_USES) {
                    val newStack = stack.copy()
                    newStack.set(
                        MOD_COMPONENTS.flaskRemainingUsesComponent,
                        flaskRemainingUses - diffLevel,
                    )
                    newStack
                  } else {
                    stack.recipeRemainder
                  }
                }
                else -> null
              }

          val newState =
              when (stackContentType) {
                    ContentType.MILK,
                    ContentType.POTION -> MOD_BLOCKS.potionCauldronBlock
                    ContentType.WATER -> Blocks.WATER_CAULDRON
                    else -> null
                  }
                  ?.defaultState
                  ?.with(LeveledCauldronBlock.LEVEL, cauldronLevel + diffLevel)

          Pair(newStack, newState)
        }

    val takeFromCauldronBehavior =
        buildCauldronActionBehavior(shouldFill = false) { stack, state, blockEntity ->
          val cauldronLevel = state.getOrEmpty(LeveledCauldronBlock.LEVEL).orElse(0)
          if (cauldronLevel < LeveledCauldronBlock.MIN_LEVEL) {
            // There is nothing to take from this cauldron.
            return@buildCauldronActionBehavior Pair(/* newState= */ null, /* newStack= */ null)
          }

          val stackItemType = getItemType(stack)
          val cauldronContentType = getBlockContentType(state, blockEntity)

          val diffLevel =
              when (stackItemType) {
                ItemType.BOTTLE -> 1
                ItemType.BUCKET ->
                    if (cauldronLevel >= LeveledCauldronBlock.MAX_LEVEL) cauldronLevel else 0
                ItemType.FLASK -> cauldronLevel
                else -> 0
              }
          if (diffLevel <= 0) {
            return@buildCauldronActionBehavior Pair(/* newState= */ null, /* newStack= */ null)
          }

          val newStack =
              when (stackItemType) {
                ItemType.BOTTLE ->
                    when (cauldronContentType) {
                      ContentType.MILK,
                      ContentType.POTION -> {
                        val newStack = Items.POTION.defaultStack
                        if (blockEntity is PotionCauldronBlockEntity) {
                          newStack.set(
                              DataComponentTypes.POTION_CONTENTS,
                              blockEntity.potionContents,
                          )
                        }
                        newStack
                      }
                      ContentType.WATER ->
                          PotionContentsComponent.createStack(Items.POTION, Potions.WATER)
                      else -> null
                    }
                ItemType.BUCKET ->
                    when (cauldronContentType) {
                      ContentType.MILK -> Items.MILK_BUCKET.defaultStack
                      ContentType.WATER -> Items.WATER_BUCKET.defaultStack
                      else -> null
                    }
                ItemType.FLASK -> {
                  val newStack =
                      when (cauldronContentType) {
                        ContentType.MILK,
                        ContentType.POTION -> {
                          val newStack = MOD_ITEMS.potionFlaskItem.defaultStack
                          if (blockEntity is PotionCauldronBlockEntity) {
                            newStack.set(
                                DataComponentTypes.POTION_CONTENTS,
                                blockEntity.potionContents,
                            )
                          }
                          newStack
                        }
                        ContentType.WATER ->
                            PotionContentsComponent.createStack(
                                MOD_ITEMS.potionFlaskItem,
                                Potions.WATER,
                            )
                        else -> null
                      }
                  newStack?.set(MOD_COMPONENTS.flaskRemainingUsesComponent, diffLevel)
                  newStack
                }
                else -> null
              }

          val newState =
              if (cauldronLevel - diffLevel < LeveledCauldronBlock.MIN_LEVEL) {
                Blocks.CAULDRON.defaultState
              } else {
                state.with(LeveledCauldronBlock.LEVEL, cauldronLevel - diffLevel)
              }

          Pair(newStack, newState)
        }

    /** Builds the corresponding behavior for filling or taking from a cauldron. */
    fun buildCauldronActionBehavior(
        shouldFill: Boolean,
        getCauldronActionResults:
            (stack: ItemStack, state: BlockState, blockEntity: BlockEntity?) -> Pair<
                    ItemStack?,
                    BlockState?,
                >,
    ) =
        CauldronBehavior {
            state: BlockState,
            world: World,
            pos: BlockPos,
            player: PlayerEntity,
            hand: Hand,
            stack: ItemStack ->
          val stackItemType = getItemType(stack)
          val stackContentType = getItemContentType(stack)
          val cauldronBlockEntity = world.getBlockEntity(pos)
          val cauldronContentType = getBlockContentType(state, cauldronBlockEntity)
          if (stackItemType == null || stackContentType == null || cauldronContentType == null) {
            // Either the source or the destination is invalid.
            return@CauldronBehavior ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
          }

          if (
              (if (shouldFill) cauldronContentType else stackContentType) != ContentType.EMPTY &&
                  stackContentType != cauldronContentType
          ) {
            // The destination is not empty, yet the source and destination contents are mismatched.
            return@CauldronBehavior ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
          }

          val stackPotionContents = stack.get(DataComponentTypes.POTION_CONTENTS)
          val cauldronPotionContents =
              (cauldronBlockEntity as? PotionCauldronBlockEntity)?.potionContents
          if (
              (if (shouldFill) cauldronPotionContents else stackPotionContents) != null &&
                  stackPotionContents != cauldronPotionContents
          ) {
            // The destination contains a potion, yet the source and destination potion contents are
            // mismatched.
            return@CauldronBehavior ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
          }

          val (newStack, newState) = getCauldronActionResults(stack, state, cauldronBlockEntity)
          if (newStack == null || newState == null) {
            // The cauldron action resulted in a no-op.
            return@CauldronBehavior ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
          }

          if (world.isClient) {
            return@CauldronBehavior ItemActionResult.success(/* swingHand= */ true)
          }

          player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, newStack))
          player.incrementStat(Stats.USE_CAULDRON)
          player.incrementStat(Stats.USED.getOrCreateStat(stack.item))

          world.setBlockState(pos, newState)
          if (
              shouldFill &&
                  !state.isOf(MOD_BLOCKS.potionCauldronBlock) &&
                  newState.isOf(MOD_BLOCKS.potionCauldronBlock)
          ) {
            world.addBlockEntity(
                PotionCauldronBlockEntity(
                    pos,
                    newState,
                    stackPotionContents ?: PotionContentsComponent.DEFAULT,
                )
            )
          }
          cauldronBlockEntity?.markDirty()
          world.updateListeners(pos, state, newState, /* flags= */ 0)

          world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(newState))
          world.playSound(
              /* source= */ null,
              pos,
              when (stackItemType) {
                ItemType.BOTTLE,
                ItemType.FLASK ->
                    if (shouldFill) SoundEvents.ITEM_BOTTLE_FILL else SoundEvents.ITEM_BOTTLE_EMPTY
                ItemType.BUCKET ->
                    if (shouldFill) SoundEvents.ITEM_BUCKET_FILL else SoundEvents.ITEM_BUCKET_EMPTY
              },
              SoundCategory.BLOCKS,
              /* volume= */ 1.0f,
              /* pitch= */ 1.0f,
          )
          world.emitGameEvent(
              /* entity= */ null,
              if (shouldFill) GameEvent.FLUID_PLACE else GameEvent.FLUID_PICKUP,
              pos,
          )

          ItemActionResult.success(/* swingHand= */ false)
        }

    /** Gets the cauldron-relevant [ItemType] of an item. */
    fun getItemType(stack: ItemStack) =
        when {
          stack.isOf(Items.GLASS_BOTTLE) || stack.isOf(Items.POTION) -> ItemType.BOTTLE
          stack.isOf(Items.BUCKET) ||
              stack.isOf(Items.MILK_BUCKET) ||
              stack.isOf(Items.WATER_BUCKET) -> ItemType.BUCKET
          stack.isOf(MOD_ITEMS.glassFlaskItem) || stack.isOf(MOD_ITEMS.potionFlaskItem) ->
              ItemType.FLASK
          else -> null
        }

    /** Gets the cauldron-relevant [ContentType] of an item. */
    fun getItemContentType(stack: ItemStack) =
        when {
          stack.isOf(Items.BUCKET) ||
              stack.isOf(Items.GLASS_BOTTLE) ||
              stack.isOf(MOD_ITEMS.glassFlaskItem) -> ContentType.EMPTY
          stack.isOf(Items.MILK_BUCKET) -> ContentType.MILK
          stack.isOf(Items.POTION) || stack.isOf(MOD_ITEMS.potionFlaskItem) -> {
            val potionContents = stack.get(DataComponentTypes.POTION_CONTENTS)
            when {
              potionContentsMatchId(potionContents, MILK_POTION_ID) -> ContentType.MILK
              potionContents?.matches(Potions.WATER) == true -> ContentType.WATER
              else -> ContentType.POTION
            }
          }
          stack.isOf(Items.WATER_BUCKET) -> ContentType.WATER
          else -> null
        }

    /** Gets the cauldron-relevant [ContentType] of a block. */
    fun getBlockContentType(state: BlockState, blockEntity: BlockEntity?) =
        when (state.block) {
          Blocks.CAULDRON -> ContentType.EMPTY
          MOD_BLOCKS.potionCauldronBlock -> {
            if (blockEntity is PotionCauldronBlockEntity) {
              val potionContents = blockEntity.potionContents
              when {
                potionContentsMatchId(potionContents, MILK_POTION_ID) -> ContentType.MILK
                potionContents.matches(Potions.WATER) -> ContentType.WATER
                else -> ContentType.POTION
              }
            } else null
          }
          Blocks.WATER_CAULDRON -> ContentType.WATER
          else -> null
        }
  }
}

@JvmField val MOD_CAULDRON_BEHAVIORS = object : ModCauldronBehaviors() {}
