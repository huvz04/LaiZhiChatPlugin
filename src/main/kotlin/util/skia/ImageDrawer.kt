package org.huvz.mirai.plugin.util.skia

import org.jetbrains.skia.Canvas

interface ImageDrawer {
    fun draw(canvas: Canvas)
}