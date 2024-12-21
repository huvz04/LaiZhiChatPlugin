package util.skia.impl

import org.jetbrains.skia.*
import org.huvz.mirai.plugin.util.skia.ImageDrawer

class BackgroundDrawer(
    private val outputWidth: Int,
    private val outputHeight: Int,
    private val titleText: String
) : ImageDrawer {
    override var fontTypeface: Typeface? = null

    override fun draw(canvas: Canvas) {
        val imagePaint = Paint().apply {
            isAntiAlias = true
        }
//        图片，已废弃
//        val inputStream = this::class.java.getResourceAsStream("/image/06.jpg")
//        val originalImage = ImageIO.read(inputStream).toImage()
//
//        // 计算缩放比例
//        val aspectRatio = originalImage.width.toFloat() / originalImage.height.toFloat()
//        val targetWidth = outputWidth.toFloat()
//        val targetHeight = targetWidth / aspectRatio
//        // 缩放原始图像并绘制到新的图像对象上
//        canvas.drawImageRect(
//            originalImage,
//            Rect.makeXYWH(0f, 0f, targetWidth, targetHeight),
//            imagePaint
//        )

        // 创建线性渐变
        val colors = intArrayOf(0xFFACB6E5.toInt(), 0xFF86FDE8.toInt()) // 渐变颜色
        val positions = floatArrayOf(0f, 1f) // 渐变位置，null 表示平均分布
        val shader2 = Shader.makeLinearGradient(
            Point(0f, 0f), // 渐变起点
            Point(outputWidth.toFloat(), outputHeight.toFloat()), // 渐变终点
            colors, // 渐变颜色
            positions, // 渐变位置
            GradientStyle.DEFAULT // 渐变样式
        )

        val paint = Paint().apply {
            shader = shader2
        }

        canvas.drawRect(Rect.makeWH(outputWidth.toFloat(), outputHeight.toFloat()), paint)
    }
}



