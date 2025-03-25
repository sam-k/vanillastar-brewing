package com.vanillastar.vsbrewing.block.entity

import com.vanillastar.vsbrewing.block.MOD_BLOCKS
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
class FlaskBlockEntity(pos: BlockPos, state: BlockState, var item: ItemStack = ItemStack.EMPTY) :
    BlockEntity(MOD_BLOCK_ENTITIES.flaskBlockEntityType, pos, state) {
  private companion object {
    const val ITEM_NBT_KEY = "item"
  }

  override fun readNbt(nbt: NbtCompound, registryLookup: WrapperLookup) {
    super.readNbt(nbt, registryLookup)
    if (nbt.contains(ITEM_NBT_KEY, NbtElement.COMPOUND_TYPE.toInt())) {
      this.item = ItemStack.fromNbtOrEmpty(registryLookup, nbt.getCompound(ITEM_NBT_KEY))
    }
    this.world?.updateListeners(this.pos, this.cachedState, this.cachedState, /* flags= */ 0)
  }

  override fun writeNbt(nbt: NbtCompound, registryLookup: WrapperLookup) {
    super.writeNbt(nbt, registryLookup)
    if (!this.item.isEmpty) {
      nbt.put(ITEM_NBT_KEY, this.item.encode(registryLookup))
    }
  }

  override fun toInitialChunkDataNbt(registryLookup: WrapperLookup): NbtCompound =
      this.createNbt(registryLookup)

  override fun toUpdatePacket(): BlockEntityUpdateS2CPacket =
      BlockEntityUpdateS2CPacket.create(this)

  override fun getRenderData() = this.item.get(DataComponentTypes.POTION_CONTENTS)?.color ?: -1
}
