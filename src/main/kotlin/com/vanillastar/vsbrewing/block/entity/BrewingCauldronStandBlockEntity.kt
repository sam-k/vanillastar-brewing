package com.vanillastar.vsbrewing.block.entity

import com.vanillastar.vsbrewing.MOD_ID
import com.vanillastar.vsbrewing.block.MOD_BLOCKS
import com.vanillastar.vsbrewing.component.MOD_COMPONENTS
import com.vanillastar.vsbrewing.item.MOD_ITEMS
import com.vanillastar.vsbrewing.networking.BrewingCauldronPayload
import com.vanillastar.vsbrewing.screen.BrewingCauldronScreenHandler
import kotlin.jvm.optionals.getOrNull
import net.fabricmc.fabric.api.networking.v1.PlayerLookup
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.SharedConstants
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.LeveledCauldronBlock
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BrewingStandBlockEntity
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
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ItemScatterer
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import net.minecraft.world.WorldEvents

val BREWING_CAULDRON_STAND_BLOCK_ENTITY_METADATA =
    ModBlockEntityMetadata("brewing_cauldron_stand", MOD_BLOCKS.brewingCauldronStandBlock)

/**
 * [BlockEntity] for the brewing stand part of a brewing cauldron.
 *
 * This is copied mostly from [BrewingStandBlockEntity], modified to take potions from a cauldron
 * block below instead of item slots within its inventory.
 */
class BrewingCauldronStandBlockEntity(pos: BlockPos, state: BlockState) :
    LockableContainerBlockEntity(
        MOD_BLOCK_ENTITIES.brewingCauldronStandBlockEntityType,
        pos,
        state,
    ),
    SidedInventory,
    ExtendedScreenHandlerFactory<BrewingCauldronPayload> {
  companion object {
    const val INVENTORY_SIZE = 2
    const val INGREDIENT_SLOT_INDEX = 0
    const val FUEL_SLOT_INDEX = 1

    const val DATA_SIZE = 2
    const val BREW_TIME_DATA_INDEX = 0
    const val FUEL_LEVEL_DATA_INDEX = 1

    const val FUEL_LEVEL_PER_ITEM = 20
    const val BREW_TIME_TICKS = 10 * SharedConstants.TICKS_PER_SECOND

    private val TOP_SLOTS = intArrayOf(INGREDIENT_SLOT_INDEX)
    private val SIDE_SLOTS = intArrayOf(FUEL_SLOT_INDEX)

    internal val ticker =
        object : BlockEntityTicker<BrewingCauldronStandBlockEntity> {
          override fun tick(
              world: World,
              pos: BlockPos,
              state: BlockState,
              blockEntity: BrewingCauldronStandBlockEntity,
          ) {
            // Refill fuel if empty.
            val fuel = blockEntity.inventory[FUEL_SLOT_INDEX]
            if (blockEntity.fuelLevel <= 0 && fuel.isOf(Items.BLAZE_POWDER)) {
              blockEntity.fuelLevel = FUEL_LEVEL_PER_ITEM
              fuel.decrement(1)
              markDirty(world, pos, state)
            }

            val isCraftable = canCraft(world, pos, blockEntity.inventory)
            val ingredient = blockEntity.inventory[INGREDIENT_SLOT_INDEX]
            if (blockEntity.brewTime > 0) {
              // Continue brewing, or halt if no longer valid.
              blockEntity.brewTime--
              if (blockEntity.brewTime == 0 && isCraftable) {
                craft(world, pos, blockEntity.inventory)
              } else if (!isCraftable || !ingredient.isOf(blockEntity.itemBrewing)) {
                blockEntity.brewTime = 0
              }
              markDirty(world, pos, state)
            } else if (isCraftable && blockEntity.fuelLevel > 0) {
              // Start crafting.
              blockEntity.fuelLevel--
              blockEntity.brewTime = BREW_TIME_TICKS
              blockEntity.itemBrewing = ingredient.item
              markDirty(world, pos, state)
            }
          }
        }

    private fun getBrewingCauldronPayload(
        pos: BlockPos,
        state: BlockState?,
        blockEntity: PotionCauldronBlockEntity?,
        player: ServerPlayerEntity?,
    ): BrewingCauldronPayload {
      val cauldronPos = pos.down()
      val resolvedBlockEntity =
          blockEntity
              ?: if (state != null && state.isOf(Blocks.WATER_CAULDRON)) {
                PotionCauldronBlockEntity(
                    cauldronPos,
                    state,
                    PotionContentsComponent(Potions.WATER),
                )
              } else {
                null
              }
      val potionNbt =
          if (player != null) resolvedBlockEntity?.createNbt(player.world.registryManager) else null
      return BrewingCauldronPayload(
          pos.asLong(),
          state?.getOrEmpty(LeveledCauldronBlock.LEVEL)?.getOrNull() ?: 0,
          potionNbt ?: NbtCompound(),
      )
    }

    private fun getCauldronPotionStack(world: World, pos: BlockPos): ItemStack {
      val cauldronPos = pos.down()

      val downState = world.getBlockState(cauldronPos)
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
          .getBlockEntity(cauldronPos, MOD_BLOCK_ENTITIES.potionCauldronBlockEntityType)
          .getOrNull()
          ?.getPotionStack() ?: ItemStack.EMPTY
    }

    private fun canCraft(world: World, pos: BlockPos, slots: DefaultedList<ItemStack>): Boolean {
      val brewingRecipeRegistry = world.brewingRecipeRegistry

      val ingredient = slots[INGREDIENT_SLOT_INDEX]
      if (ingredient.isEmpty || !brewingRecipeRegistry.isValidIngredient(ingredient)) {
        return false
      }

      return brewingRecipeRegistry.hasRecipe(this.getCauldronPotionStack(world, pos), ingredient)
    }

    private fun craft(world: World, pos: BlockPos, slots: DefaultedList<ItemStack>) {
      val brewingRecipeRegistry = world.brewingRecipeRegistry

      var ingredientStack = slots[INGREDIENT_SLOT_INDEX]
      val cauldronPotionStack = this.getCauldronPotionStack(world, pos)
      val resultingPotionStack = brewingRecipeRegistry.craft(ingredientStack, cauldronPotionStack)

      // Update potion cauldron, creating one in place of a water cauldron if necessary.
      val cauldronPos = pos.down()
      val cauldronState = world.getBlockState(cauldronPos)
      var newCauldronState = cauldronState
      var potionCauldronBlockEntity =
          world
              .getBlockEntity(cauldronPos, MOD_BLOCK_ENTITIES.potionCauldronBlockEntityType)
              .orElse(null)
      if (cauldronState.isOf(Blocks.WATER_CAULDRON) && potionCauldronBlockEntity == null) {
        newCauldronState =
            MOD_BLOCKS.potionCauldronBlock.defaultState.with(
                LeveledCauldronBlock.LEVEL,
                cauldronState.get(LeveledCauldronBlock.LEVEL),
            )
        world.setBlockState(cauldronPos, newCauldronState, Block.NOTIFY_LISTENERS)
        potionCauldronBlockEntity = PotionCauldronBlockEntity(cauldronPos, newCauldronState)
        world.addBlockEntity(potionCauldronBlockEntity)
      }
      potionCauldronBlockEntity.setPotion(resultingPotionStack)
      markDirty(world, cauldronPos, newCauldronState)
      world.updateListeners(cauldronPos, cauldronState, newCauldronState, /* flags= */ 0)
      val brewingCauldronStandBlockEntity =
          world
              .getBlockEntity(pos, MOD_BLOCK_ENTITIES.brewingCauldronStandBlockEntityType)
              .getOrNull()
      if (brewingCauldronStandBlockEntity != null) {
        for (player in PlayerLookup.tracking(brewingCauldronStandBlockEntity)) {
          ServerPlayNetworking.send(
              player,
              getBrewingCauldronPayload(
                  cauldronPos,
                  newCauldronState,
                  potionCauldronBlockEntity,
                  player,
              ),
          )
        }
      }

      ingredientStack.decrement(1)
      if (ingredientStack.item.hasRecipeRemainder()) {
        val ingredientRemainder = ItemStack(ingredientStack.item.recipeRemainder)
        if (ingredientRemainder.isEmpty) {
          ingredientStack = ingredientRemainder
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

      slots[INGREDIENT_SLOT_INDEX] = ingredientStack
      world.syncWorldEvent(WorldEvents.BREWING_STAND_BREWS, pos, /* data= */ 0)
    }
  }

  private var brewTime = 0
  private var fuelLevel = 0
  private var itemBrewing: Item? = null
  private var inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)

  private val propertyDelegate =
      object : PropertyDelegate {
        override fun get(index: Int): Int =
            when (index) {
              BREW_TIME_DATA_INDEX -> this@BrewingCauldronStandBlockEntity.brewTime
              FUEL_LEVEL_DATA_INDEX -> this@BrewingCauldronStandBlockEntity.fuelLevel
              else -> 0
            }

        override fun set(index: Int, value: Int) {
          when (index) {
            BREW_TIME_DATA_INDEX -> this@BrewingCauldronStandBlockEntity.brewTime = value
            FUEL_LEVEL_DATA_INDEX -> this@BrewingCauldronStandBlockEntity.fuelLevel = value
          }
        }

        override fun size() = DATA_SIZE
      }

  override fun getContainerName(): Text = Text.translatable("container.${MOD_ID}.brewing_cauldron")

  override fun size() = this.inventory.size

  override fun getHeldStacks(): DefaultedList<ItemStack> = this.inventory

  override fun setHeldStacks(inventory: DefaultedList<ItemStack>) {
    this.inventory = inventory
  }

  override fun createScreenHandler(syncId: Int, playerInventory: PlayerInventory) =
      BrewingCauldronScreenHandler(
          syncId,
          playerInventory,
          this.getScreenOpeningData(null),
          this,
          this.propertyDelegate,
      )

  override fun getAvailableSlots(side: Direction) =
      when (side) {
        Direction.UP -> TOP_SLOTS
        else -> SIDE_SLOTS
      }

  override fun isValid(slot: Int, stack: ItemStack) =
      when (slot) {
        INGREDIENT_SLOT_INDEX ->
            (this.world?.brewingRecipeRegistry ?: BrewingRecipeRegistry.EMPTY).isValidIngredient(
                stack
            )
        FUEL_SLOT_INDEX -> stack.isOf(Items.BLAZE_POWDER)
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

  override fun getScreenOpeningData(player: ServerPlayerEntity?): BrewingCauldronPayload {
    val cauldronPos = this.pos.down()
    return getBrewingCauldronPayload(
        cauldronPos,
        player?.world?.getBlockState(cauldronPos),
        player
            ?.world
            ?.getBlockEntity(cauldronPos, MOD_BLOCK_ENTITIES.potionCauldronBlockEntityType)
            ?.getOrNull(),
        player,
    )
  }
}
