package com.vanillastar.vsbrewing.gui

import com.mojang.blaze3d.systems.RenderSystem
import com.vanillastar.vsbrewing.block.MOD_BLOCKS
import com.vanillastar.vsbrewing.block.entity.MOD_BLOCK_ENTITIES
import com.vanillastar.vsbrewing.block.entity.PotionCauldronBlockEntity
import com.vanillastar.vsbrewing.screen.BrewingCauldronScreenHandler
import com.vanillastar.vsbrewing.utils.getModIdentifier
import kotlin.jvm.optionals.getOrNull
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.block.LeveledCauldronBlock
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.client.render.OverlayTexture
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import org.joml.Quaternionf

@Environment(EnvType.CLIENT)
class BrewingCauldronScreen(
    handler: BrewingCauldronScreenHandler,
    val inventory: PlayerInventory,
    title: Text,
) : HandledScreen<BrewingCauldronScreenHandler>(handler, inventory, title) {
  private companion object {
    val TEXTURE = getModIdentifier("textures/gui/container/brewing_cauldron.png")
    val FUEL_LENGTH_TEXTURE: Identifier =
        Identifier.ofVanilla("container/brewing_stand/fuel_length")
    val BREW_PROGRESS_TEXTURE: Identifier =
        Identifier.ofVanilla("container/brewing_stand/brew_progress")
    val BUBBLES_TEXTURE: Identifier = Identifier.ofVanilla("container/brewing_stand/bubbles")
    val BUBBLE_PROGRESS = intArrayOf(29, 24, 20, 16, 11, 6, 0)
  }

  var previewBlockEntity =
      PotionCauldronBlockEntity(BlockPos.ORIGIN, MOD_BLOCKS.potionCauldronPreviewBlock.defaultState)

  override fun init() {
    super.init()
    this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2
  }

  override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
    super.render(context, mouseX, mouseY, delta)
    this.drawMouseoverTooltip(context, mouseX, mouseY)

    val referenceBlockEntity =
        this.inventory.player.world
            .getBlockEntity(this.handler.data.pos, MOD_BLOCK_ENTITIES.potionCauldronBlockEntityType)
            .getOrNull()
    if (referenceBlockEntity != null) {
      this.previewBlockEntity =
          PotionCauldronBlockEntity(
              BlockPos.ORIGIN,
              MOD_BLOCKS.potionCauldronPreviewBlock.defaultState.with(
                  LeveledCauldronBlock.LEVEL,
                  referenceBlockEntity.state.get(LeveledCauldronBlock.LEVEL),
              ),
          )
      this.previewBlockEntity.potionContents = referenceBlockEntity.potionContents
    }
  }

  override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
    val x = (this.width - this.backgroundWidth) / 2
    val y = (this.height - this.backgroundHeight) / 2
    context.drawTexture(
        TEXTURE,
        x,
        y,
        /* u= */ 0,
        /* v= */ 0,
        this.backgroundWidth,
        this.backgroundHeight,
    )

    val remainingFuelWidth =
        MathHelper.clamp((18 * this.handler.getFuel() + 20 - 1) / 20, /* min= */ 0, /* max= */ 18)
    if (remainingFuelWidth > 0) {
      context.drawGuiTexture(
          FUEL_LENGTH_TEXTURE,
          /* i= */ 18,
          /* j= */ 4,
          /* k= */ 0,
          /* l= */ 0,
          x + 60,
          y + 44,
          remainingFuelWidth,
          /* height= */ 4,
      )
    }

    val brewTime = this.handler.getBrewTime()
    if (brewTime > 0) {
      val brewProgressHeight = (28.0f * (1.0f - brewTime / 400.0f)).toInt()
      if (brewProgressHeight > 0) {
        context.drawGuiTexture(
            BREW_PROGRESS_TEXTURE,
            /* i= */ 9,
            /* j= */ 28,
            /* k= */ 0,
            /* l= */ 0,
            x + 97,
            y + 16,
            /* width= */ 9,
            brewProgressHeight,
        )
      }
      val bubbleProgressHeight = BUBBLE_PROGRESS[(brewTime / 2) % BUBBLE_PROGRESS.size]
      if (bubbleProgressHeight > 0) {
        context.drawGuiTexture(
            BUBBLES_TEXTURE,
            /* i= */ 12,
            /* j= */ 29,
            /* k= */ 0,
            /* l= */ 29 - bubbleProgressHeight,
            x + 63,
            y + 14 + 29 - bubbleProgressHeight,
            /* width= */ 12,
            bubbleProgressHeight,
        )
      }
    }

    context.matrices.push()

    val scale = 24.0f
    context.matrices.translate(x + 128.0, y + 64.0, scale * 2.0)
    context.matrices.scale(scale, scale, scale)
    context.matrices.multiply(
        Quaternionf()
            .rotateX(-30 * Math.PI.toFloat() / 180)
            .rotateY(135 * Math.PI.toFloat() / 180)
            .rotateZ(Math.PI.toFloat())
    )

    val instance = MinecraftClient.getInstance()
    RenderSystem.enableCull()
    instance.blockRenderManager.renderBlockAsEntity(
        this.previewBlockEntity.state,
        context.matrices,
        context.vertexConsumers,
        LightmapTextureManager.MAX_LIGHT_COORDINATE,
        OverlayTexture.DEFAULT_UV,
    )
    instance.blockEntityRenderDispatcher
        .get(this.previewBlockEntity)
        ?.render(
            this.previewBlockEntity,
            delta,
            context.matrices,
            context.vertexConsumers,
            LightmapTextureManager.MAX_LIGHT_COORDINATE,
            OverlayTexture.DEFAULT_UV,
        )
    RenderSystem.disableCull()

    context.matrices.pop()
  }
}
