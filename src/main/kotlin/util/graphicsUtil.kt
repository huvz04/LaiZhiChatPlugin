package org.huvz.mirai.plugin.util

import org.jetbrains.skia.*
import org.huvz.mirai.plugin.entity.ImageData
import java.io.File
import java.io.FileOutputStream
import org.jetbrains.skia.EncodedImageFormat

class graphicsUtil {


    companion object{


        private const val numImagesPerRow = 4
        private const val imageWidth = 185f
        private const val imageHeight = 185f
        private const val imageSpacing = 20f
        private const val imageTextHeight = 35f
        private const val cornerRadius = 10f
        private const val titleHeight = 120f

        //fun draw(fileList: MutableList<ImageData>): ExternalResource? {
        //下方为本地测试用的
        fun draw(fileList: MutableList<ImageData>): File {
            val title = "图库列表"


            val numRows = (fileList.size + numImagesPerRow - 1) / numImagesPerRow
            val outputWidth = numImagesPerRow * (imageWidth + imageSpacing) + imageSpacing
            val outputHeight = numRows * (imageHeight + imageSpacing + imageTextHeight + 10) + imageSpacing + titleHeight;

            val surface = Surface.makeRasterN32Premul(outputWidth.toInt(), outputHeight.toInt())
            val canvas = surface.canvas

            // #F0F8FF
            canvas.clear(Color.makeRGB(240,248,255))

            // 绘制标题
            val titleFont = Font(Typeface.makeFromName("微软雅黑",FontStyle.BOLD), 60f)
            val titlePaint = Paint().apply {
                color = Color.BLACK
            }
            val titleWidth = titleFont.measureText(title)
            val titleX = (outputWidth - titleWidth.width) / 2;
            val titleY = titleHeight / 2
            canvas.drawString(title, titleX, titleY,titleFont, titlePaint)
            val paint = Paint().apply {
                strokeWidth = strokeWidth
                color = Color.BLACK
            }

            canvas.drawLine(30f, titleHeight-20, outputWidth-30f, titleHeight-20, paint)



            // 绘制图片预览
            val imagePaint = Paint().apply {
                isAntiAlias = true
            }

            for (i in fileList.indices) {
                val file = fileList[i]
                val row = i / numImagesPerRow
                val col = i % numImagesPerRow

                val x = imageSpacing + col * (imageWidth + imageSpacing)
                //10是预留的多余空间
                val y = titleHeight + imageSpacing + row * (imageHeight + imageSpacing + imageTextHeight + 10)

                // 绘制圆角矩形背景
                val roundRect = RRect.makeXYWH(
                    x, y, imageWidth, imageHeight+imageTextHeight,
                    cornerRadius
                )
                val backgroundPaint = Paint().apply {
                    //淡蓝
                    color = Color.makeRGB(173,216,230)
                }

                canvas.drawRRect(roundRect, backgroundPaint)
                // 绘制图片名称
                val textFont = Font(Typeface.makeFromName("微软雅黑",FontStyle.BOLD), 18f)

                val textPaint = Paint().apply {
                    color = Color.BLACK
                }


                val textX = x + (imageWidth - textFont.measureText(file.ckname).width) / 2
                val textY = y + (imageHeight- textFont.measureText(file.ckname).height) + 35f
                canvas.drawString(file.ckname, textX, textY,textFont, textPaint)




                try {
                    // 绘制图片
                    val fileData = file.Img?.readBytes()
                    val image = fileData?.let { Image.makeFromEncoded(it) }
                    image?.let {
                        // 计算图像的宽高比
                        val imageAspectRatio = image.width.toFloat() / image.height.toFloat()

                        // 计算目标框的宽高比
                        val targetAspectRatio = 185f / 185f

                        // 计算缩放比例
                        val scale = if (imageAspectRatio > targetAspectRatio) {
                            // 图像宽度超出目标框的宽度，按照目标框的宽度进行缩放
                            185f / image.width.toFloat()
                        } else {
                            // 图像高度超出目标框的高度，按照目标框的高度进行缩放
                            185f / image.height.toFloat()
                        }

                        // 计算缩放后的图像尺寸
                        val scaledWidth = (image.width.toFloat() * scale).toInt()
                        val scaledHeight = (image.height.toFloat() * scale).toInt()

                        // 计算绘制图像的起始坐标
                        val drawX = x + (imageWidth - scaledWidth) / 2
                        val drawY = y + (imageHeight - scaledHeight) / 2

                        // 绘制图像
                        canvas.drawImageRect(it, Rect.makeXYWH(drawX, drawY, scaledWidth.toFloat(), scaledHeight.toFloat()
                        ), imagePaint)
                    }
                } catch (e: Exception) {
                    canvas.drawString("渲染失败！", x + (imageWidth / 2), y + (imageHeight - 10) / 2, textFont, textPaint)
                    println("图片出现错误哦；来自${file.ckname}的图片渲染失败")
                }




                val circlePaint = Paint().apply {
                    color = Color.makeRGB(178,34,34)
                    mode = PaintMode.FILL
                }
                //绘制右上角的圆
                canvas.drawCircle(x+ imageWidth-5,y+5,20f,circlePaint)
                val textCirclePaint = Paint().apply {
                    color = Color.WHITE
                    Paint
                }
                val Font1 = Font(Typeface.makeFromName("微软雅黑",FontStyle.BOLD), 20f)
                val textWith1 = Font1.measureText(file.size.toString())
                canvas.drawString(file.size.toString(), x+ imageWidth-textWith1.width/2-5, y + textWith1.height/2+5, Font1,textCirclePaint)

            }

            val tempFile = File.createTempFile("image", ".png")
            val outputStream = FileOutputStream(tempFile)
            val image = surface.makeImageSnapshot()
            val encoded = image.encodeToData(EncodedImageFormat.PNG, 100)
            encoded?.use { data ->
                outputStream.write(data.bytes)
            }

            outputStream.close()
            surface.close()

            //val externalResource = tempFile.toExternalResource()
            //tempFile.delete()
            return tempFile
        }
    }
}