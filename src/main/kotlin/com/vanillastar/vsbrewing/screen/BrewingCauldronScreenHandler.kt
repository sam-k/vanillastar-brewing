package com.vanillastar.vsbrewing.screen

import com.vanillastar.vsbrewing.block.entity.BREWING_STAND_INVENTORY_SIZE
import com.vanillastar.vsbrewing.networking.BrewingCauldronPayload
import net.minecraft.block.entity.BrewingStandBlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.recipe.BrewingRecipeRegistry
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.BrewingStandScreenHandler
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot

const val BREWING_CAULDRON_SCREEN_HANDLER_NAME = "brewing_cauldron"

/**
 * [ScreenHandler] for the brewing cauldron GUI.
 *
 * This is copied mostly from [BrewingStandScreenHandler], modified to take potions from a cauldron
 * block below instead of item slots within its inventory.
 */
class BrewingCauldronScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory,
    internal var data: BrewingCauldronPayload = BrewingCauldronPayload(0, 0, NbtCompound()),
    private val inventory: Inventory = SimpleInventory(BREWING_STAND_INVENTORY_SIZE),
    private val propertyDelegate: PropertyDelegate =
        ArrayPropertyDelegate(BrewingStandBlockEntity.PROPERTY_COUNT),
) : ScreenHandler(MOD_SCREEN_HANDLERS.brewingCauldronScreenHandler, syncId) {
  private companion object {
    /** Displayed slot index for the ingredient item. */
    const val INGREDIENT_DISPLAY_SLOT_INDEX = 0

    /** Displayed slot index for the fuel item. */
    const val FUEL_DISPLAY_SLOT_INDEX = 1

    /** Effective inventory size for a brewing cauldron, which lacks potion slots. */
    const val BREWING_CAULDRON_DISPLAY_INVENTORY_SIZE = 2
  }

  private val brewingRecipeRegistry: BrewingRecipeRegistry =
      this.playerInventory.player.world.brewingRecipeRegistry

  private val ingredientSlot =
      object :
          Slot(this.inventory, BrewingStandBlockEntity.INPUT_SLOT_INDEX, /* x= */ 79, /* y= */ 17) {
        override fun canInsert(stack: ItemStack) =
            this@BrewingCauldronScreenHandler.brewingRecipeRegistry.isValidIngredient(stack)
      }

  private val fuelSlot =
      object :
          Slot(this.inventory, BrewingStandBlockEntity.FUEL_SLOT_INDEX, /* x= */ 17, /* y= */ 17) {
        override fun canInsert(stack: ItemStack) = stack.isOf(Items.BLAZE_POWDER)
      }

  init {
    checkSize(inventory, BREWING_STAND_INVENTORY_SIZE)
    checkDataCount(propertyDelegate, BrewingStandBlockEntity.PROPERTY_COUNT)

    this.addSlot(this.ingredientSlot)
    this.addSlot(this.fuelSlot)
    this.addProperties(propertyDelegate)
    for (row in 0..2) {
      for (col in 0..8) {
        this.addSlot(
            Slot(
                this.playerInventory,
                /* index= */ col + row * 9 + 9,
                /* x= */ 8 + col * 18,
                /* y= */ 84 + row * 18,
            )
        )
      }
    }
    for (col in 0..8) {
      this.addSlot(
          Slot(this.playerInventory, /* index= */ col, /* x= */ 8 + col * 18, /* y= */ 142)
      )
    }
  }

  fun getBrewTime() = this.propertyDelegate.get(BrewingStandBlockEntity.BREW_TIME_PROPERTY_INDEX)

  fun getFuel() = this.propertyDelegate.get(BrewingStandBlockEntity.FUEL_PROPERTY_INDEX)

  override fun canUse(player: PlayerEntity) = this.inventory.canPlayerUse(player)

  override fun quickMove(player: PlayerEntity, slotIndex: Int): ItemStack {
    var stack = ItemStack.EMPTY
    val slot: Slot? = this.slots[slotIndex]
    if (slot == null || !slot.hasStack()) {
      return stack
    }

    val newStack = slot.stack
    stack = newStack.copy()

    if (slotIndex < BREWING_CAULDRON_DISPLAY_INVENTORY_SIZE) {
      // Slot is in brewing cauldron inventory. Try quick-moving into player inventory.
      if (
          !this.insertItem(
              newStack,
              BREWING_CAULDRON_DISPLAY_INVENTORY_SIZE,
              this.slots.size,
              /* fromLast= */ true,
          )
      ) {
        return ItemStack.EMPTY // Quick-move failed
      }
      slot.onQuickTransfer(newStack, stack)
    } else {
      // Slot is in player inventory. Try quick-moving into brewing cauldron inventory first.
      val isFuel = this.fuelSlot.canInsert(stack)
      val isIngredient = this.ingredientSlot.canInsert(stack)
      if (isFuel && isIngredient) {
        if (
            this.tryQuickMoveIntoFuelSlot(newStack) ||
                !this.tryQuickMoveIntoIngredientSlot(newStack)
        ) {
          return ItemStack.EMPTY // Quick-move failed
        }
      } else if (isFuel) {
        if (!this.tryQuickMoveIntoFuelSlot(newStack)) {
          return ItemStack.EMPTY // Quick-move failed
        }
      } else if (isIngredient) {
        if (!this.tryQuickMoveIntoIngredientSlot(newStack)) {
          return ItemStack.EMPTY // Quick-move failed
        }
      } else if (slotIndex < BREWING_CAULDRON_DISPLAY_INVENTORY_SIZE + 27) {
        // Slot is in player storage. Try quick-moving into player hotbar.
        if (
            !this.insertItem(
                newStack,
                BREWING_CAULDRON_DISPLAY_INVENTORY_SIZE + 27,
                BREWING_CAULDRON_DISPLAY_INVENTORY_SIZE + 36,
                /* fromLast= */ false,
            )
        ) {
          return ItemStack.EMPTY // Quick-move failed
        }
      } else if (slotIndex < BREWING_CAULDRON_DISPLAY_INVENTORY_SIZE + 36) {
        // Slot is in player hotbar. Try quick-moving into player storage.
        if (
            !this.insertItem(
                newStack,
                BREWING_CAULDRON_DISPLAY_INVENTORY_SIZE,
                BREWING_CAULDRON_DISPLAY_INVENTORY_SIZE + 27,
                /* fromLast= */ false,
            )
        ) {
          return ItemStack.EMPTY // Quick-move failed
        }
      } else if (
          !this.insertItem(
              newStack,
              BREWING_CAULDRON_DISPLAY_INVENTORY_SIZE,
              BREWING_CAULDRON_DISPLAY_INVENTORY_SIZE + 36,
              false,
          )
      ) {
        // Try quick-moving into player inventory.
        return ItemStack.EMPTY // Quick-move failed
      }
    }

    if (newStack.isEmpty) {
      slot.stack = ItemStack.EMPTY
    } else {
      slot.markDirty()
    }

    if (newStack.count == stack.count) {
      return ItemStack.EMPTY // Quick-move was no-op
    }

    slot.onTakeItem(player, stack)
    return stack
  }

  private fun tryQuickMoveIntoFuelSlot(stack: ItemStack) =
      this.insertItem(
          stack,
          FUEL_DISPLAY_SLOT_INDEX,
          FUEL_DISPLAY_SLOT_INDEX + 1,
          /* fromLast= */ false,
      )

  private fun tryQuickMoveIntoIngredientSlot(stack: ItemStack) =
      this.insertItem(
          stack,
          INGREDIENT_DISPLAY_SLOT_INDEX,
          INGREDIENT_DISPLAY_SLOT_INDEX + 1,
          /* fromLast= */ false,
      )
}
