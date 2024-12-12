package util.skia.impl

import org.jetbrains.skia.*
import org.huvz.mirai.plugin.util.skia.ImageDrawer

class MaskDrawer(
    private val outputWidth: Int,
    private val outputHeight: Int,
    private val infoHeight:Int,
    private val lt:Float

) : ImageDrawer {
    override fun draw(canvas: Canvas) {

        val centerX = outputWidth / 2f
        val centerY = outputHeight / 2f
        val cornerRadius = 20f
        // 上间遮罩层
        val maskRRectInfo  = RRect.makeXYWH(
            lt,
            lt,
            (outputWidth - lt*2).toFloat(),
            infoHeight.toFloat(),
            cornerRadius
        )


        // 中间遮罩层
        val maskRRectMain  = RRect.makeXYWH(
            centerX - (outputWidth / 2 - lt)   ,
            centerY - (outputHeight / 2 - lt) + infoHeight + 25,
            (outputWidth - 80).toFloat(),
            (outputHeight - 80).toFloat() - infoHeight,
            cornerRadius
        )
        val mainPaint = Paint().apply {
            color = Color.makeARGB(138, 255, 255, 255) // 白色半透明底色
        }
        canvas.drawRRect(maskRRectInfo, mainPaint)
        canvas.drawRRect(maskRRectMain, mainPaint)
        val paint = Paint().apply {
            color4f = Color4f(0f,0f,0f,255f,)
            strokeWidth = 1f // 设置画笔宽度为 3 像素
            strokeJoin = PaintStrokeJoin.ROUND
            mode = PaintMode.STROKE
            imageFilter = ImageFilter.makeBlur(2f,2f,FilterTileMode.REPEAT)
        }
        canvas.drawPath(
            Path().apply {
                addRRect(maskRRectInfo)
            }
            ,paint)
        canvas.drawPath(
            Path().apply {
                addRRect(maskRRectMain)
            }
            ,paint)



    }
}