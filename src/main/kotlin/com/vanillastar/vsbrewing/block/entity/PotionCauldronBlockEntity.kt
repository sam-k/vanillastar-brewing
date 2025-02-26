package com.vanillastar.vsbrewing.block.entity

import com.vanillastar.vsbrewing.block.MOD_BLOCKS
import com.vanillastar.vsbrewing.block.PotionCauldronBlock
import com.vanillastar.vsbrewing.component.MOD_COMPONENTS
import com.vanillastar.vsbrewing.item.MOD_ITEMS
import com.vanillastar.vsbrewing.utils.getLogger
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtOps
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.registry.RegistryWrapper.WrapperLookup
import net.minecraft.util.math.BlockPos

val POTION_CAULDRON_BLOCK_ENTITY_METADATA =
    ModBlockEntityMetadata("potion_cauldron", MOD_BLOCKS.potionCauldronBlock)

class PotionCauldronBlockEntity(
    pos: BlockPos,
    val state: BlockState,
    var potionContents: PotionContentsComponent = PotionContentsComponent.DEFAULT,
) : BlockEntity(MOD_BLOCK_ENTITIES.potionCauldronBlockEntityType, pos, state) {
  private val logger = getLogger()

  fun getPotionStack(): ItemStack {
    val stack = ItemStack(MOD_ITEMS.potionFlaskItem)
    stack.set(DataComponentTypes.POTION_CONTENTS, this.potionContents)
    stack.set(
        MOD_COMPONENTS.potionFlaskRemainingUsesComponent,
        state.get(PotionCauldronBlock.LEVEL),
    )
    return stack
  }

  fun setPotion(stack: ItemStack) {
    val potionContents = stack.get(DataComponentTypes.POTION_CONTENTS)
    this.potionContents = potionContents ?: PotionContentsComponent.DEFAULT
  }

  override fun readNbt(nbt: NbtCompound, registryLookup: WrapperLookup) {
    super.readNbt(nbt, registryLookup)
    if (nbt.contains("potion_contents")) {
      PotionContentsComponent.CODEC.parse(
              registryLookup.getOps(NbtOps.INSTANCE),
              nbt.get("potion_contents"),
          )
          .resultOrPartial { logger.warn("Failed to parse potion cauldron content: {}", it) }
          .ifPresent { this.potionContents = it }
    }
    // Send update if this data is set programmatically.
    this.world?.updateListeners(this.pos, this.cachedState, this.cachedState, /* flags= */ 0)
  }

  override fun writeNbt(nbt: NbtCompound, registryLookup: WrapperLookup) {
    super.writeNbt(nbt, registryLookup)
    if (this.potionContents != PotionContentsComponent.DEFAULT) {
      nbt.put(
          "potion_contents",
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
