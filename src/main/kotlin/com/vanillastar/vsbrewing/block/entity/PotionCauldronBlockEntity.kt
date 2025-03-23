package com.vanillastar.vsbrewing.block.entity

import com.vanillastar.vsbrewing.block.MOD_BLOCKS
import com.vanillastar.vsbrewing.component.MOD_COMPONENTS
import com.vanillastar.vsbrewing.item.MOD_ITEMS
import com.vanillastar.vsbrewing.utils.getLogger
import net.minecraft.block.BlockState
import net.minecraft.block.LeveledCauldronBlock
import net.minecraft.block.entity.BlockEntity
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtOps
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.registry.RegistryWrapper.WrapperLookup
import net.minecraft.util.math.BlockPos

val POTION_CAULDRON_BLOCK_ENTITY_METADATA =
    ModBlockEntityMetadata(
        "potion_cauldron",
        MOD_BLOCKS.potionCauldronBlock,
        MOD_BLOCKS.potionCauldronPreviewBlock,
    )

/** [BlockEntity] for a potion-filled cauldron. */
class PotionCauldronBlockEntity(
    pos: BlockPos,
    val state: BlockState,
    var potionContents: PotionContentsComponent,
) : BlockEntity(MOD_BLOCK_ENTITIES.potionCauldronBlockEntityType, pos, state) {
  private companion object {
    val LOGGER = getLogger()

    const val POTION_CONTENTS_NBT_KEY = "potion_contents"
  }

  constructor(pos: BlockPos, state: BlockState) : this(pos, state, PotionContentsComponent.DEFAULT)

  fun getPotionStack(): ItemStack {
    val stack = ItemStack(MOD_ITEMS.potionFlaskItem)
    stack.set(DataComponentTypes.POTION_CONTENTS, this.potionContents)
    stack.set(MOD_COMPONENTS.flaskRemainingUsesComponent, state.get(LeveledCauldronBlock.LEVEL))
    return stack
  }

  fun setPotion(stack: ItemStack) {
    val potionContents = stack.get(DataComponentTypes.POTION_CONTENTS)
    this.potionContents = potionContents ?: PotionContentsComponent.DEFAULT
  }

  fun readNbt(nbt: NbtCompound, registryLookup: WrapperLookup, sendUpdate: Boolean) {
    super.readNbt(nbt, registryLookup)
    if (nbt.contains(POTION_CONTENTS_NBT_KEY, NbtElement.COMPOUND_TYPE.toInt())) {
      PotionContentsComponent.CODEC.parse(
              registryLookup.getOps(NbtOps.INSTANCE),
              nbt.getCompound(POTION_CONTENTS_NBT_KEY),
          )
          .resultOrPartial { LOGGER.warn("Failed to parse potion cauldron content: {}", it) }
          .ifPresent { this.potionContents = it }
    }
    // Send update, for example if this data is set programmatically.
    if (sendUpdate) {
      this.world?.updateListeners(this.pos, this.cachedState, this.cachedState, /* flags= */ 0)
    }
  }

  override fun readNbt(nbt: NbtCompound, registryLookup: WrapperLookup) {
    this.readNbt(nbt, registryLookup, sendUpdate = true)
  }

  override fun writeNbt(nbt: NbtCompound, registryLookup: WrapperLookup) {
    super.writeNbt(nbt, registryLookup)
    if (this.potionContents != PotionContentsComponent.DEFAULT) {
      nbt.put(
          POTION_CONTENTS_NBT_KEY,
          PotionContentsComponent.CODEC.encodeStart(
                  registryLookup.getOps(NbtOps.INSTANCE),
                  this.potionContents,
              )
              .getOrThrow(),
      )
    }
  }

  override fun toInitialChunkDataNbt(registryLookup: WrapperLookup): NbtCompound =
      this.createNbt(registryLookup)

  override fun toUpdatePacket(): BlockEntityUpdateS2CPacket =
      BlockEntityUpdateS2CPacket.create(this)

  override fun getRenderData() = this.potionContents.color
}
