package com.vanillastar.vsbrewing.utils

import com.vanillastar.vsbrewing.block.MOD_BLOCKS
import com.vanillastar.vsbrewing.block.entity.PotionCauldronBlockEntity
import com.vanillastar.vsbrewing.item.MOD_ITEMS
import com.vanillastar.vsbrewing.potion.MILK_POTION_ID
import com.vanillastar.vsbrewing.potion.potionContentsMatchId
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.potion.Potions
import net.minecraft.registry.Registries

enum class PotionVariant {
  NORMAL,
  SPLASH,
  LINGERING;

  companion object {
    fun get(stack: ItemStack) =
        when {
          stack.isOf(Items.MILK_BUCKET) ||
              stack.isOf(Items.POTION) ||
              stack.isOf(Items.WATER_BUCKET) ||
              stack.isOf(MOD_ITEMS.potionFlaskItem) -> NORMAL
          stack.isOf(Items.SPLASH_POTION) || stack.isOf(MOD_ITEMS.splashPotionFlaskItem) -> SPLASH
          stack.isOf(Items.LINGERING_POTION) || stack.isOf(MOD_ITEMS.lingeringPotionFlaskItem) ->
              LINGERING
          else -> null
        }

    fun getItem(type: PotionVariant, isFlask: Boolean): Item =
        when (type) {
          NORMAL -> if (isFlask) MOD_ITEMS.potionFlaskItem else Items.POTION
          SPLASH -> if (isFlask) MOD_ITEMS.splashPotionFlaskItem else Items.SPLASH_POTION
          LINGERING -> if (isFlask) MOD_ITEMS.lingeringPotionFlaskItem else Items.LINGERING_POTION
        }
  }
}

enum class PotionItemType {
  BOTTLE,
  BUCKET,
  FLASK;

  companion object {
    /** Gets the cauldron-relevant potion item type of an item. */
    fun get(stack: ItemStack) =
        when {
          stack.isOf(Items.GLASS_BOTTLE) || stack.isOf(Items.POTION) -> BOTTLE
          stack.isOf(Items.BUCKET) ||
              stack.isOf(Items.MILK_BUCKET) ||
              stack.isOf(Items.WATER_BUCKET) -> BUCKET
          stack.isOf(MOD_ITEMS.glassFlaskItem) || stack.isOf(MOD_ITEMS.potionFlaskItem) -> FLASK
          else -> null
        }
  }
}

enum class PotionContentType {
  EMPTY,
  MILK,
  POTION,
  WATER;

  companion object {
    /** Gets the cauldron-relevant potion content type of an item. */
    fun get(stack: ItemStack) =
        when {
          stack.isOf(Items.BUCKET) ||
              stack.isOf(Items.GLASS_BOTTLE) ||
              stack.isOf(MOD_ITEMS.glassFlaskItem) -> EMPTY
          stack.isOf(Items.MILK_BUCKET) -> MILK
          stack.isOf(Items.POTION) || stack.isOf(MOD_ITEMS.potionFlaskItem) -> {
            val potionContents = stack.get(DataComponentTypes.POTION_CONTENTS)
            when {
              potionContentsMatchId(potionContents, MILK_POTION_ID) -> MILK
              potionContents?.matches(Potions.WATER) == true -> WATER
              else -> POTION
            }
          }
          stack.isOf(Items.WATER_BUCKET) -> WATER
          else -> null
        }

    /** Gets the cauldron-relevant potion content type of a block. */
    fun get(state: BlockState, blockEntity: PotionCauldronBlockEntity?) =
        when (state.block) {
          Blocks.CAULDRON -> EMPTY
          MOD_BLOCKS.potionCauldronBlock -> {
            if (blockEntity != null) {
              val potionContents = blockEntity.potionContents
              when {
                potionContentsMatchId(potionContents, MILK_POTION_ID) -> MILK
                potionContents.matches(Potions.WATER) -> WATER
                else -> POTION
              }
            } else null
          }
          Blocks.WATER_CAULDRON -> WATER
          else -> null
        }

    /** Gets the default [PotionContentsComponent] given a cauldron-relevant potion content type. */
    fun getPotionContents(type: PotionContentType) =
        when (type) {
          MILK -> PotionContentsComponent(Registries.POTION.getEntry(MILK_POTION_ID).get())
          POTION -> PotionContentsComponent.DEFAULT
          WATER -> PotionContentsComponent(Potions.WATER)
          else -> null
        }
  }
}
