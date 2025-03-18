package com.vanillastar.vsbrewing.block.entity

import com.vanillastar.vsbrewing.block.MOD_BLOCKS
import com.vanillastar.vsbrewing.utils.getLogger
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtOps
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.registry.RegistryWrapper.WrapperLookup
import net.minecraft.util.math.BlockPos

val FLASK_BLOCK_ENTITY_METADATA = ModBlockEntityMetadata("flask", MOD_BLOCKS.flaskBlock)

/** [BlockEntity] for a placed flask. */
class FlaskBlockEntity(
    pos: BlockPos,
    state: BlockState,
    var potionContents: PotionContentsComponent? = null,
) : BlockEntity(MOD_BLOCK_ENTITIES.flaskBlockEntityType, pos, state) {
  private companion object {
    val LOGGER = getLogger()
  }

  override fun readNbt(nbt: NbtCompound, registryLookup: WrapperLookup) {
    super.readNbt(nbt, registryLookup)
    if (nbt.contains("potion_contents")) {
      PotionContentsComponent.CODEC.parse(
              registryLookup.getOps(NbtOps.INSTANCE),
              nbt.get("potion_contents"),
          )
          .resultOrPartial { LOGGER.warn("Failed to parse flask potion contents: {}", it) }
          .ifPresent { this.potionContents = it }
    }
    this.world?.updateListeners(this.pos, this.cachedState, this.cachedState, /* flags= */ 0)
  }

  override fun writeNbt(nbt: NbtCompound, registryLookup: WrapperLookup) {
    super.writeNbt(nbt, registryLookup)
    if (this.potionContents != null && this.potionContents != PotionContentsComponent.DEFAULT) {
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

  override fun getRenderData() = potionContents?.color
}
