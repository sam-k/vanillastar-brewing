package com.vanillastar.vsbrewing.block.entity

import com.vanillastar.vsbrewing.block.MOD_BLOCKS
import com.vanillastar.vsbrewing.block.PotionCauldronBlock
import com.vanillastar.vsbrewing.component.MOD_COMPONENTS
import com.vanillastar.vsbrewing.item.MOD_ITEMS
import kotlin.jvm.optionals.getOrNull
import net.minecraft.SharedConstants
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.LeveledCauldronBlock
import net.minecraft.block.entity.LockableContainerBlockEntity
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.potion.Potions
import net.minecraft.recipe.BrewingRecipeRegistry
import net.minecraft.registry.RegistryWrapper.WrapperLookup
import net.minecraft.screen.PropertyDelegate
import net.minecraft.text.Text
import net.minecraft.util.ItemScatterer
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import net.minecraft.world.WorldEvents

val BREWING_CAULDRON_STAND_BLOCK_ENTITY_METADATA =
    ModBlockEntityMetadata("brewing_cauldron_stand", MOD_BLOCKS.brewingCauldronStandBlock)

class BrewingCauldronStandBlockEntity(pos: BlockPos, state: BlockState) :
    LockableContainerBlockEntity(
        MOD_BLOCK_ENTITIES.brewingCauldronStandBlockEntityType,
        pos,
        state,
    ),
    SidedInventory {
  companion object {
    private const val INGREDIENT_SLOT = 0
    private const val FUEL_SLOT = 1

    private val TOP_SLOTS: IntArray = intArrayOf(INGREDIENT_SLOT)
    private val SIDE_SLOTS: IntArray = intArrayOf(FUEL_SLOT)

    private const val FUEL_LEVEL_PER_ITEM = 20

    internal fun tick(
        world: World,
        pos: BlockPos,
        state: BlockState,
        blockEntity: BrewingCauldronStandBlockEntity,
    ) {
      // Refill fuel if empty.
      val fuel = blockEntity.inventory[FUEL_SLOT]
      if (blockEntity.fuelLevel <= 0 && fuel.isOf(Items.BLAZE_POWDER)) {
        blockEntity.fuelLevel = FUEL_LEVEL_PER_ITEM
        fuel.decrement(1)
        markDirty(world, pos, state)
      }

      val isCraftable = this.canCraft(world, pos, blockEntity.inventory)
      val ingredient = blockEntity.inventory[INGREDIENT_SLOT]
      if (blockEntity.brewTime > 0) {
        // Continue brewing, or halt if no longer valid.
        blockEntity.brewTime--
        if (blockEntity.brewTime == 0 && isCraftable) {
          this.craft(world, pos, blockEntity.inventory)
        } else if (!isCraftable || !ingredient.isOf(blockEntity.itemBrewing)) {
          blockEntity.brewTime = 0
        }
        markDirty(world, pos, state)
      } else if (isCraftable && blockEntity.fuelLevel > 0) {
        // Start crafting.
        blockEntity.fuelLevel--
        blockEntity.brewTime = 20 * SharedConstants.TICKS_PER_SECOND
        blockEntity.itemBrewing = ingredient.item
        markDirty(world, pos, state)
      }
    }

    private fun getCauldronPotionStack(world: World, pos: BlockPos): ItemStack {
      val posDown = pos.down()

      val downState = world.getBlockState(posDown)
      if (downState.isOf(Blocks.WATER_CAULDRON)) {
        val stack = ItemStack(MOD_ITEMS.potionFlaskItem)
        stack.set(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent(Potions.WATER))
        stack.set(
            MOD_COMPONENTS.potionFlaskRemainingUsesComponent,
            downState.get(LeveledCauldronBlock.LEVEL),
        )
        return stack
      }

      return world
          .getBlockEntity(posDown, MOD_BLOCK_ENTITIES.potionCauldronBlockEntityType)
          .getOrNull()
          ?.getPotionStack() ?: ItemStack.EMPTY
    }

    private fun canCraft(world: World, pos: BlockPos, slots: DefaultedList<ItemStack>): Boolean {
      val brewingRecipeRegistry = world.brewingRecipeRegistry

      val ingredient = slots[INGREDIENT_SLOT]
      if (ingredient.isEmpty || !brewingRecipeRegistry.isValidIngredient(ingredient)) {
        return false
      }

      return brewingRecipeRegistry.hasRecipe(this.getCauldronPotionStack(world, pos), ingredient)
    }

    private fun craft(world: World, pos: BlockPos, slots: DefaultedList<ItemStack>) {
      val brewingRecipeRegistry = world.brewingRecipeRegistry

      var ingredient = slots[INGREDIENT_SLOT]
      val cauldronPotionStack = this.getCauldronPotionStack(world, pos)
      val resultingPotionStack = brewingRecipeRegistry.craft(ingredient, cauldronPotionStack)

      // Update potion cauldron, creating one in place of a water cauldron if necessary.
      val posDown = pos.down()
      val downState = world.getBlockState(posDown)
      var potionCauldronBlockEntity =
          world
              .getBlockEntity(posDown, MOD_BLOCK_ENTITIES.potionCauldronBlockEntityType)
              .orElse(null)
      if (downState.isOf(Blocks.WATER_CAULDRON) && potionCauldronBlockEntity == null) {
        val newDownState =
            MOD_BLOCKS.potionCauldronBlock.defaultState.with(
                PotionCauldronBlock.LEVEL,
                downState.get(LeveledCauldronBlock.LEVEL),
            )
        world.setBlockState(posDown, newDownState, Block.NOTIFY_LISTENERS)
        potionCauldronBlockEntity = PotionCauldronBlockEntity(posDown, newDownState)
        world.addBlockEntity(potionCauldronBlockEntity)
        markDirty(world, posDown, newDownState)
        world.updateListeners(pos, downState, newDownState, /* flags= */ 0)
      }
      potionCauldronBlockEntity?.setPotion(resultingPotionStack)

      if (ingredient.item.hasRecipeRemainder()) {
        val ingredientRemainder = ItemStack(ingredient.item.recipeRemainder)
        if (ingredientRemainder.isEmpty) {
          ingredient = ingredientRemainder
        } else {
          ItemScatterer.spawn(
              world,
              pos.x.toDouble(),
              pos.y.toDouble(),
              pos.z.toDouble(),
              ingredientRemainder,
          )
        }
      }

      slots[INGREDIENT_SLOT] = ingredient
      world.syncWorldEvent(WorldEvents.BREWING_STAND_BREWS, pos, /* data= */ 0)
    }
  }

  private var brewTime = 0
  private var fuelLevel = 0
  private var itemBrewing: Item? = null
  private var inventory = DefaultedList.ofSize(2, ItemStack.EMPTY)

  private val propertyDelegate =
      object : PropertyDelegate {
        override fun get(index: Int) =
            when (index) {
              0 -> this@BrewingCauldronStandBlockEntity.brewTime
              1 -> this@BrewingCauldronStandBlockEntity.fuelLevel
              else -> 0
            }

        override fun set(index: Int, value: Int) {
          when (index) {
            0 -> this@BrewingCauldronStandBlockEntity.brewTime = value
            1 -> this@BrewingCauldronStandBlockEntity.fuelLevel = value
          }
        }

        override fun size(): Int {
          return 2
        }
      }

  override fun getContainerName(): Text = Text.translatable("container.brewing_cauldron_stand")

  override fun size() = this.inventory.size

  override fun getHeldStacks(): DefaultedList<ItemStack> = this.inventory

  override fun setHeldStacks(inventory: DefaultedList<ItemStack>) {
    this.inventory = inventory
  }

  // TODO: Implement BrewingCauldronStandScreenHandler.
  override fun createScreenHandler(syncId: Int, playerInventory: PlayerInventory) = null

  override fun getAvailableSlots(side: Direction) =
      when (side) {
        Direction.UP -> TOP_SLOTS
        else -> SIDE_SLOTS
      }

  override fun isValid(slot: Int, stack: ItemStack) =
      when (slot) {
        INGREDIENT_SLOT ->
            (this.world?.brewingRecipeRegistry ?: BrewingRecipeRegistry.EMPTY).isValidIngredient(
                stack
            )
        FUEL_SLOT -> stack.isOf(Items.BLAZE_POWDER)
        else -> false
      }

  override fun canInsert(slot: Int, stack: ItemStack, dir: Direction?) = this.isValid(slot, stack)

  override fun canExtract(slot: Int, stack: ItemStack, dir: Direction) = false

  override fun readNbt(nbt: NbtCompound, registryLookup: WrapperLookup) {
    super.readNbt(nbt, registryLookup)
    this.inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY)
    Inventories.readNbt(nbt, this.inventory, registryLookup)
    this.brewTime = nbt.getShort("BrewTime").toInt()
    this.fuelLevel = nbt.getByte("Fuel").toInt()
  }

  override fun writeNbt(nbt: NbtCompound, registryLookup: WrapperLookup) {
    super.writeNbt(nbt, registryLookup)
    Inventories.writeNbt(nbt, this.inventory, registryLookup)
    nbt.putShort("BrewTime", this.brewTime.toShort())
    nbt.putByte("Fuel", this.fuelLevel.toByte())
  }
}
