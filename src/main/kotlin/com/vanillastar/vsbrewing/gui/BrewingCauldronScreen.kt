package com.vanillastar.vsbrewing.gui

import com.mojang.blaze3d.systems.RenderSystem
import com.vanillastar.vsbrewing.block.MOD_BLOCKS
import com.vanillastar.vsbrewing.block.entity.PotionCauldronBlockEntity
import com.vanillastar.vsbrewing.screen.BrewingCauldronScreenHandler
import com.vanillastar.vsbrewing.utils.getModIdentifier
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.LeveledCauldronBlock
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.RenderLayers
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ColorHelper
import net.minecraft.util.math.MathHelper
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.locationtech.jts.algorithm.ConvexHull
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.impl.CoordinateArraySequence

@Environment(EnvType.CLIENT)
class BrewingCauldronScreen(
    handler: BrewingCauldronScreenHandler,
    inventory: PlayerInventory,
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

    val PREVIEW_CAULDRON_PROJECTION_UNIT_CORNERS =
        arrayOf(
            Vector3f(0.0f, 0.0f, 0.0f),
            Vector3f(0.0f, 1.0f, 0.0f),
            Vector3f(0.0f, 1.0f, 1.0f),
            Vector3f(1.0f, 0.0f, 0.0f),
            Vector3f(1.0f, 0.0f, 1.0f),
            Vector3f(1.0f, 1.0f, 0.0f),
            Vector3f(1.0f, 1.0f, 1.0f),
        )
    val GEOMETRY_FACTORY = GeometryFactory()
  }

  private var previewCauldronPositionMatrix = Matrix4f()

  override fun init() {
    super.init()
    this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2
  }

  override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
    super.render(context, mouseX, mouseY, delta)
    this.drawMouseoverTooltip(context, mouseX, mouseY)
    this.drawCauldronBlockTooltip(context, mouseX, mouseY)
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

    // Prepare the matrix stack.
    val scale = 24.0f
    context.matrices.translate(x + 162.0, y + 64.0, scale * 2.0)
    context.matrices.scale(scale, scale, -scale)
    context.matrices.multiply(
        Quaternionf()
            .rotateX(30 * Math.PI.toFloat() / 180)
            .rotateY(-45 * Math.PI.toFloat() / 180)
            .rotateZ(Math.PI.toFloat())
    )
    this.previewCauldronPositionMatrix = Matrix4f(context.matrices.peek().positionMatrix)

    // Render the cauldron block.
    RenderSystem.enableCull()
    val previewBlockState = this.getPreviewCauldronBlockState()
    val previewBlockEntity = this.getPreviewCauldronBlockEntity(previewBlockState)
    val color = previewBlockEntity.potionContents.color
    this.client!!
        .blockRenderManager
        .blockModelRenderer
        .render(
            context.matrices.peek(),
            context.vertexConsumers.getBuffer(RenderLayers.getBlockLayer(previewBlockState)),
            previewBlockState,
            this.client!!.blockRenderManager.getModel(previewBlockState),
            ColorHelper.Argb.getRed(color) / 255.0f,
            ColorHelper.Argb.getGreen(color) / 255.0f,
            ColorHelper.Argb.getBlue(color) / 255.0f,
            LightmapTextureManager.MAX_LIGHT_COORDINATE,
            OverlayTexture.DEFAULT_UV,
        )
    this.client!!
        .blockEntityRenderDispatcher
        .get(previewBlockEntity)
        ?.render(
            previewBlockEntity,
            delta,
            context.matrices,
            context.vertexConsumers,
            LightmapTextureManager.MAX_LIGHT_COORDINATE,
            OverlayTexture.DEFAULT_UV,
        )
    RenderSystem.disableCull()

    context.matrices.pop()
  }

  private fun drawCauldronBlockTooltip(context: DrawContext, mouseX: Int, mouseY: Int) {
    if (!this.handler.cursorStack.isEmpty) {
      return
    }

    val level = this.handler.data.level
    if (level < LeveledCauldronBlock.MIN_LEVEL || level > LeveledCauldronBlock.MAX_LEVEL) {
      return
    }

    val projectionCornerCoords =
        PREVIEW_CAULDRON_PROJECTION_UNIT_CORNERS.map {
              this.previewCauldronPositionMatrix.transformPosition(Vector3f(it))
            }
            .map { Coordinate(it.x.toDouble(), it.y.toDouble()) }
            .toTypedArray()
    val mouseCoord =
        Point(
            CoordinateArraySequence(arrayOf(Coordinate(mouseX.toDouble(), mouseY.toDouble()))),
            GEOMETRY_FACTORY,
        )
    if (!ConvexHull(projectionCornerCoords, GEOMETRY_FACTORY).convexHull.contains(mouseCoord)) {
      return
    }

    val previewPotionStack = this.getPreviewCauldronBlockEntity().getPotionStack()
    context.drawTooltip(
        this.textRenderer,
        this.getTooltipFromItem(previewPotionStack),
        previewPotionStack.tooltipData,
        mouseX,
        mouseY,
    )
  }

  private fun getPreviewCauldronBlockState(): BlockState {
    val level = this.handler.data.level
    if (level < LeveledCauldronBlock.MIN_LEVEL || level > LeveledCauldronBlock.MAX_LEVEL) {
      return Blocks.CAULDRON.defaultState
    }
    return MOD_BLOCKS.potionCauldronPreviewBlock.defaultState.with(
        LeveledCauldronBlock.LEVEL,
        level,
    )
  }

  private fun getPreviewCauldronBlockEntity() =
      this.getPreviewCauldronBlockEntity(this.getPreviewCauldronBlockState())

  private fun getPreviewCauldronBlockEntity(state: BlockState): PotionCauldronBlockEntity {
    val previewBlockEntity =
        PotionCauldronBlockEntity(BlockPos.fromLong(this.handler.data.packedPos), state)
    previewBlockEntity.readNbt(
        this.handler.data.potionCauldronNbt,
        this.client!!.world!!.registryManager,
        /* sendUpdate= */ false,
    )
    return previewBlockEntity
  }
}
