package com.vanillastar.vsbrewing.screen

import com.vanillastar.vsbrewing.block.entity.BrewingCauldronStandBlockEntity
import com.vanillastar.vsbrewing.networking.BrewingCauldronPayload
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.recipe.BrewingRecipeRegistry
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot

const val BREWING_CAULDRON_SCREEN_HANDLER_NAME = "brewing_cauldron"

class BrewingCauldronScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory,
    internal var data: BrewingCauldronPayload,
    private val inventory: Inventory =
        SimpleInventory(BrewingCauldronStandBlockEntity.INVENTORY_SIZE),
    private val propertyDelegate: PropertyDelegate =
        ArrayPropertyDelegate(BrewingCauldronStandBlockEntity.DATA_SIZE),
) : ScreenHandler(MOD_SCREEN_HANDLERS.brewingCauldronScreenHandler, syncId) {
  private val brewingRecipeRegistry: BrewingRecipeRegistry =
      this.playerInventory.player.world.brewingRecipeRegistry

  private val ingredientSlot =
      object :
          Slot(
              this.inventory,
              BrewingCauldronStandBlockEntity.INGREDIENT_SLOT_INDEX,
              /* x= */ 79,
              /* y= */ 17,
          ) {
        override fun canInsert(stack: ItemStack) =
            this@BrewingCauldronScreenHandler.brewingRecipeRegistry.isValidIngredient(stack)
      }

  private val fuelSlot =
      object :
          Slot(
              this.inventory,
              BrewingCauldronStandBlockEntity.FUEL_SLOT_INDEX,
              /* x= */ 17,
              /* y= */ 17,
          ) {
        override fun canInsert(stack: ItemStack) = stack.isOf(Items.BLAZE_POWDER)
      }

  init {
    checkSize(inventory, BrewingCauldronStandBlockEntity.INVENTORY_SIZE)
    checkDataCount(propertyDelegate, BrewingCauldronStandBlockEntity.DATA_SIZE)

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

  fun getBrewTime() =
      this.propertyDelegate.get(BrewingCauldronStandBlockEntity.BREW_TIME_DATA_INDEX)

  fun getFuel() = this.propertyDelegate.get(BrewingCauldronStandBlockEntity.FUEL_LEVEL_DATA_INDEX)

  override fun canUse(player: PlayerEntity) = this.inventory.canPlayerUse(player)

  override fun quickMove(player: PlayerEntity, slotIndex: Int): ItemStack {
    var stack = ItemStack.EMPTY
    val slot: Slot? = this.slots[slotIndex]
    if (slot == null || !slot.hasStack()) {
      return stack
    }

    val newStack = slot.stack
    stack = newStack.copy()

    val inventorySize = this.inventory.size()
    if (slotIndex < inventorySize) {
      // Slot is in brewing cauldron inventory. Try quick-moving into player inventory.
      if (!this.insertItem(newStack, inventorySize, this.slots.size, /* fromLast= */ true)) {
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
      } else if (slotIndex < inventorySize + 27) {
        // Slot is in player storage. Try quick-moving into player hotbar.
        if (
            !this.insertItem(
                newStack,
                inventorySize + 27,
                inventorySize + 36,
                /* fromLast= */ false,
            )
        ) {
          return ItemStack.EMPTY // Quick-move failed
        }
      } else if (slotIndex < inventorySize + 36) {
        // Slot is in player hotbar. Try quick-moving into player storage.
        if (!this.insertItem(newStack, inventorySize, inventorySize + 27, /* fromLast= */ false)) {
          return ItemStack.EMPTY // Quick-move failed
        }
      } else if (!this.insertItem(newStack, inventorySize, inventorySize + 36, false)) {
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
          BrewingCauldronStandBlockEntity.FUEL_SLOT_INDEX,
          BrewingCauldronStandBlockEntity.FUEL_SLOT_INDEX + 1,
          /* fromLast= */ false,
      )

  private fun tryQuickMoveIntoIngredientSlot(stack: ItemStack) =
      this.insertItem(
          stack,
          BrewingCauldronStandBlockEntity.INGREDIENT_SLOT_INDEX,
          BrewingCauldronStandBlockEntity.INGREDIENT_SLOT_INDEX + 1,
          /* fromLast= */ false,
      )
}
