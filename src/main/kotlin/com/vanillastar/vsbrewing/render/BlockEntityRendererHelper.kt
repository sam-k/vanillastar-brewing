package com.vanillastar.vsbrewing.render

import net.minecraft.client.model.ModelPartBuilder
import net.minecraft.client.model.ModelTransform
import net.minecraft.util.math.Direction

fun getModelPartBuilder(
    dir: Direction,
    size: Float,
    uMap: Map<Direction, Int>,
    vMap: Map<Direction, Int>,
) = getModelPartBuilder(dir, size, size, size, uMap, vMap)

/**
 * Automatically builds and gets the [ModelPartBuilder] corresponding to the given specifications.
 *
 * This assumes 16x16 textures for the UV map.
 */
fun getModelPartBuilder(
    dir: Direction,
    sizeX: Float,
    sizeY: Float,
    sizeZ: Float,
    uMap: Map<Direction, Int>,
    vMap: Map<Direction, Int>,
): ModelPartBuilder {
  // Using only down, north and west directions allows signs of Y-offsets to be consistent for all
  // cuboid faces.
  val normalizedDir =
      when (dir) {
        Direction.DOWN,
        Direction.UP -> Direction.DOWN
        Direction.NORTH,
        Direction.SOUTH -> Direction.NORTH
        Direction.WEST,
        Direction.EAST -> Direction.WEST
      }
  return ModelPartBuilder.create()
      .uv(uMap[normalizedDir] ?: 0, vMap[normalizedDir] ?: 0)
      .cuboid(
          /* offsetX= */ -0.5f * sizeX,
          /* offsetY= */ -0.5f * sizeY,
          /* offsetZ= */ -0.5f * sizeZ,
          if (normalizedDir != Direction.WEST) sizeX else 0.0f,
          if (normalizedDir != Direction.DOWN) sizeY else 0.0f,
          if (normalizedDir != Direction.NORTH) sizeZ else 0.0f,
          setOf(normalizedDir),
      )
}

fun getModelTransform(dir: Direction, pivotX: Float, pivotY: Float, pivotZ: Float) =
    getModelTransform(
        pivotX,
        pivotY,
        pivotZ,
        pitch = dir == Direction.UP || dir == Direction.WEST || dir == Direction.EAST,
        yaw = dir == Direction.SOUTH || dir == Direction.EAST,
        roll = dir == Direction.NORTH || dir == Direction.SOUTH,
    )

/**
 * Automatically builds and gets the [ModelTransform] corresponding to the given specifications.
 *
 * This supports only simple 180-degree rotations around the principal axes, which is enough for our
 * purposes. This also assumes the origin of rotation is already set at the center of the cuboid.
 */
fun getModelTransform(
    pivotX: Float,
    pivotY: Float,
    pivotZ: Float,
    pitch: Boolean = false,
    yaw: Boolean = false,
    roll: Boolean = false,
): ModelTransform =
    ModelTransform.of(
        pivotX,
        pivotY,
        pivotZ,
        if (pitch) Math.PI.toFloat() else 0.0f,
        if (yaw) Math.PI.toFloat() else 0.0f,
        if (roll) Math.PI.toFloat() else 0.0f,
    )
