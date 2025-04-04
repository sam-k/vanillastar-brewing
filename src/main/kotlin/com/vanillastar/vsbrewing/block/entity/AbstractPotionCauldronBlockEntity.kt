package com.vanillastar.vsbrewing.block.entity

import com.vanillastar.vsbrewing.component.MOD_COMPONENTS
import com.vanillastar.vsbrewing.potion.MILK_POTION_ID
import com.vanillastar.vsbrewing.potion.potionContentsMatchId
import com.vanillastar.vsbrewing.utils.PotionContentType
import com.vanillastar.vsbrewing.utils.PotionVariant
import com.vanillastar.vsbrewing.utils.getLogger
import net.minecraft.block.BlockState
import net.minecraft.block.LeveledCauldronBlock
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtOps
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.registry.RegistryWrapper.WrapperLookup
import net.minecraft.util.math.BlockPos

/** [BlockEntity] for a potion-filled cauldron. */
abstract class AbstractPotionCauldronBlockEntity(
    blockEntityType: BlockEntityType<out AbstractPotionCauldronBlockEntity>,
    pos: BlockPos,
    val state: BlockState,
    var potionContents: PotionContentsComponent,
    var variant: PotionVariant,
    val forcedLevel: Int? = null,
) : BlockEntity(blockEntityType, pos, state) {
  data class RenderData(val contentType: PotionContentType, val color: Int)

  protected companion object {
    val LOGGER = getLogger()

    const val VARIANT_TYPE_NBT_KEY = "variant_type"
    const val POTION_CONTENTS_NBT_KEY = "potion_contents"
  }

  fun getPotionStack(isFlask: Boolean): ItemStack {
    val stack = (PotionVariant.getItem(this.variant, isFlask)).defaultStack
    stack.set(DataComponentTypes.POTION_CONTENTS, this.potionContents)
    if (isFlask) {
      stack.set(
          MOD_COMPONENTS.flaskRemainingUsesComponent,
          this.forcedLevel ?: state.getOrEmpty(LeveledCauldronBlock.LEVEL).orElse(0),
      )
    }
    return stack
  }

  fun setPotion(stack: ItemStack) {
    this.variant = PotionVariant.get(stack) ?: PotionVariant.NORMAL
    this.potionContents =
        stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT)
  }

  fun readNbt(nbt: NbtCompound, registryLookup: WrapperLookup, sendUpdate: Boolean) {
    super.readNbt(nbt, registryLookup)
    if (nbt.contains(VARIANT_TYPE_NBT_KEY, NbtElement.STRING_TYPE.toInt())) {
      this.variant = PotionVariant.valueOf(nbt.getString(VARIANT_TYPE_NBT_KEY))
    }
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
    nbt.putString(VARIANT_TYPE_NBT_KEY, this.variant.toString())
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

  override fun getRenderData() =
      when {
        potionContentsMatchId(this.potionContents, MILK_POTION_ID) ->
            RenderData(PotionContentType.MILK, color = -1)
        else -> RenderData(PotionContentType.POTION, this.potionContents.color)
      }
}
