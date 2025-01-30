package com.vanillastar.vsbrewing.utils

abstract class ModRegistry {
  val logger = getLogger()

  abstract fun initialize()
}
