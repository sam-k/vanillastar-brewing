package com.vanillastar.vsbrewing.block.entity

import com.vanillastar.vsbrewing.block.MOD_BLOCKS
import com.vanillastar.vsbrewing.item.MOD_ITEMS
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.component.DataComponentTypes
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.registry.RegistryWrapper.WrapperLookup
import net.minecraft.util.math.BlockPos

val FLASK_BLOCK_ENTITY_METADATA = ModBlockEntityMetadata("flask", MOD_BLOCKS.flaskBlock)

/** [BlockEntity] for a placed flask of any kind. */
class FlaskBlockEntity(pos: BlockPos, state: BlockState, var stack: ItemStack = defaultStack) :
    BlockEntity(MOD_BLOCK_ENTITIES.flaskBlockEntityType, pos, state) {
  private companion object {
    const val ITEM_STACK_NBT_KEY = "item_stack"

    val defaultStack: ItemStack = MOD_ITEMS.glassFlaskItem.defaultStack
  }

  fun setDefaultStack() {
    this.stack = defaultStack
  }

  override fun readNbt(nbt: NbtCompound, registryLookup: WrapperLookup) {
    super.readNbt(nbt, registryLookup)
    if (nbt.contains(ITEM_STACK_NBT_KEY, NbtElement.COMPOUND_TYPE.toInt())) {
      this.stack = ItemStack.fromNbtOrEmpty(registryLookup, nbt.getCompound(ITEM_STACK_NBT_KEY))
    }
    this.world?.updateListeners(this.pos, this.cachedState, this.cachedState, /* flags= */ 0)
  }

  override fun writeNbt(nbt: NbtCompound, registryLookup: WrapperLookup) {
    super.writeNbt(nbt, registryLookup)
    if (!this.stack.isEmpty) {
      nbt.put(ITEM_STACK_NBT_KEY, this.stack.encode(registryLookup))
    }
  }

  override fun toInitialChunkDataNbt(registryLookup: WrapperLookup): NbtCompound =
      this.createNbt(registryLookup)

  override fun toUpdatePacket(): BlockEntityUpdateS2CPacket =
      BlockEntityUpdateS2CPacket.create(this)

  override fun getRenderData() = this.stack.get(DataComponentTypes.POTION_CONTENTS)?.color ?: -1
}
