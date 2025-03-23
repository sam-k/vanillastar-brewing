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
 * This assumes 16x16 textures for the UV map. This also accepts only down, north and west as valid
 * directions, as this allows signs of Y-offsets to be consistent for all cuboid faces.
 */
fun getModelPartBuilder(
    dir: Direction,
    sizeX: Float,
    sizeY: Float,
    sizeZ: Float,
    uMap: Map<Direction, Int>,
    vMap: Map<Direction, Int>,
): ModelPartBuilder {
  val directions = setOf(Direction.DOWN, Direction.NORTH, Direction.WEST)
  if (dir !in directions) {
    throw IllegalArgumentException("Direction $dir is not in allowed set of directions $directions")
  }
  return ModelPartBuilder.create()
      .uv(uMap[dir] ?: 0, vMap[dir] ?: 0)
      .cuboid(
          -0.5f * sizeX,
          -0.5f * sizeY,
          -0.5f * sizeZ,
          if (dir != Direction.WEST) sizeX else 0.0f,
          if (dir != Direction.DOWN) sizeY else 0.0f,
          if (dir != Direction.NORTH) sizeZ else 0.0f,
          setOf(dir),
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
