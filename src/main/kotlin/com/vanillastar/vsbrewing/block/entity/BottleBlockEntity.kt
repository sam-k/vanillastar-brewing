package com.vanillastar.vsbrewing.block.entity

import com.google.common.primitives.ImmutableIntArray
import com.vanillastar.vsbrewing.block.BottleBlock
import com.vanillastar.vsbrewing.block.MOD_BLOCKS
import com.vanillastar.vsbrewing.tag.MOD_TAGS
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.registry.RegistryWrapper.WrapperLookup
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ColorHelper.Argb

val BOTTLE_BLOCK_ENTITY_METADATA = ModBlockEntityMetadata("bottle", MOD_BLOCKS.bottleBlock)

/** [BlockEntity] for placed bottles of any type. */
class BottleBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(MOD_BLOCK_ENTITIES.bottleBlockEntityType, pos, state) {
  data class RenderData(val bodyColors: ImmutableIntArray, val corkColors: ImmutableIntArray)

  private companion object {
    const val ITEMS_NBT_KEY = "items"
  }

  private val items: MutableList<ItemStack> = mutableListOf()

  fun iterateItems() = items.asIterable()

  fun canInsert(stack: ItemStack) =
      this.items.size < BottleBlock.MAX_COUNT && stack.isIn(MOD_TAGS.placeableBottles)

  fun insert(stack: ItemStack) {
    if (this.canInsert(stack)) {
      this.items.add(stack)
    }
  }

  override fun readNbt(nbt: NbtCompound, registryLookup: WrapperLookup) {
    super.readNbt(nbt, registryLookup)
    if (nbt.contains(ITEMS_NBT_KEY, NbtElement.LIST_TYPE.toInt())) {
      val nbtList = nbt.getList(ITEMS_NBT_KEY, NbtElement.COMPOUND_TYPE.toInt())
      this.items.clear()
      this.items.addAll(
          nbtList.map { ItemStack.fromNbt(registryLookup, it).orElse(ItemStack.EMPTY) }
      )
    }
    this.world?.updateListeners(this.pos, this.cachedState, this.cachedState, /* flags= */ 0)
  }

  override fun writeNbt(nbt: NbtCompound, registryLookup: WrapperLookup) {
    super.writeNbt(nbt, registryLookup)
    val nbtList = NbtList()
    nbtList.addAll(this.items.map { it.encodeAllowEmpty(registryLookup) })
    if (!nbtList.isEmpty()) {
      nbt.put(ITEMS_NBT_KEY, nbtList)
    }
  }

  override fun toInitialChunkDataNbt(registryLookup: WrapperLookup): NbtCompound =
      this.createNbt(registryLookup)

  override fun toUpdatePacket(): BlockEntityUpdateS2CPacket =
      BlockEntityUpdateS2CPacket.create(this)

  override fun getRenderData(): RenderData {
    val bodyColorsBuilder = ImmutableIntArray.builder()
    val corkColorsBuilder = ImmutableIntArray.builder()
    for (stack in this.items) {
      bodyColorsBuilder.add(
          when {
            stack.isOf(Items.EXPERIENCE_BOTTLE) ->
                Argb.getArgb(/* red= */ 78, /* green= */ 61, /* blue= */ 17)
            stack.isOf(Items.OMINOUS_BOTTLE) ->
                Argb.getArgb(/* red= */ 93, /* green= */ 158, /* blue= */ 169)
            else -> -1
          }
      )
      corkColorsBuilder.add(
          when {
            stack.isOf(Items.EXPERIENCE_BOTTLE) ->
                Argb.getArgb(/* red= */ 43, /* green= */ 33, /* blue= */ 7)
            stack.isOf(Items.OMINOUS_BOTTLE) ->
                Argb.getArgb(/* red= */ 187, /* green= */ 164, /* blue= */ 159)
            else -> -1
          }
      )
    }
    return RenderData(bodyColorsBuilder.build(), corkColorsBuilder.build())
  }
}
