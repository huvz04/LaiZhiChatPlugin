package org.huvz.mirai.plugin.util.skia

import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Typeface

interface ImageDrawer {
     var fontTypeface: Typeface?;
    fun draw(canvas: Canvas)
}