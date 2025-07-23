package org.huvz.mirai.plugin.util.skia.impl

import org.huvz.mirai.plugin.PluginMain
import org.huvz.mirai.plugin.entity.ImageFile
import org.huvz.mirai.plugin.util.skia.ImageDrawer
import org.jetbrains.skia.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream


class GalleryDetailDrawer(
    private val galleryName: String,
    private val imageList: List<ImageFile>,
    private val outputWidth: Int = 1430,
    private val numImagesPerRow: Int = 6,
    private val infoHeight: Int = 100,
    private val lt: Float = 40f,
    private val targetSize: Float = 185f
) : ImageDrawer {
    override var fontTypeface: Typeface? = null

    fun draw(): File {
        // 计算输出高度，与ImagePreviewDrawer保持一致
        val rows = (imageList.size + numImagesPerRow - 1) / numImagesPerRow
        val outputHeight = infoHeight + lt * 2 + rows * (targetSize + lt) + lt
        
        val surface = Surface.makeRasterN32Premul(outputWidth, outputHeight.toInt())
        val canvas = surface.canvas
        
        // 移除背景绘制，由BackgroundDrawer负责
        
        draw(canvas)

        val tempFile = File.createTempFile("gallery_detail", ".png")
        val outputStream = FileOutputStream(tempFile)
        val image = surface.makeImageSnapshot()
        val encoded = image.encodeToData(EncodedImageFormat.PNG, 110)
        encoded?.use { data ->
            outputStream.write(data.bytes)
        }
        outputStream.close()
        surface.close()
        
        return tempFile
    }

    override fun draw(canvas: Canvas) {
        // 加载字体，与ImageDrawerComposer保持一致
        val fontStream: InputStream? = this.javaClass.classLoader.getResourceAsStream("MiSans-Demibold.ttf")
        val typeface = if (fontStream != null) {
            val fontData = Data.makeFromBytes(fontStream.readBytes())
            Typeface.makeFromData(fontData)
        } else {
            Typeface.makeFromName("微软雅黑", FontStyle.BOLD)
        }

        val titleFont = Font(typeface, 60f)
        val textFont = Font(typeface, 18f)
        val titlePaint = Paint().apply { color = Color.BLACK }
        
        // 绘制标题
        val title = "图库详情: $galleryName (${imageList.size}张图片)"
        val titleWidth = titleFont.measureText(title)
        val titleX = (outputWidth - titleWidth.width) / 2
        val titleY = infoHeight / 2f + lt + 25f
        canvas.drawString(title, titleX, titleY, titleFont, titlePaint)

        // 绘制分割线，与ImageDrawerComposer保持一致
//        val linePaint = Paint().apply {
//            color = Color.BLACK
//            strokeWidth = 1f
//        }
//        canvas.drawLine(30f, infoHeight - 20f, outputWidth - 30f, infoHeight - 20f, linePaint)

        // 绘制图片网格，使用与ImagePreviewDrawer一致的布局逻辑
        var currentX = lt
        var currentY = infoHeight.toFloat() + lt * 2
        var rowCount = 0

        for (i in imageList.indices) {
            val imageFile = imageList[i]

            try {
                // 创建背景画笔
                val backgroundPaint = Paint().apply { 
                    color = Color.makeRGB(240, 248, 255)
                    isAntiAlias = true
                }
                
                // 优先使用ImageFile中的data字段
                if (imageFile.data != null && imageFile.data!!.isNotEmpty()) {
                    // 创建临时文件用于drawImageDetail方法
                    val tempFile = File.createTempFile("temp_image", ".png")
                    tempFile.writeBytes(imageFile.data!!)
                    
                    // 绘制统一大小的图片详情
                    val imageDetail = drawImageDetail(backgroundPaint, tempFile)
                    canvas.drawImage(imageDetail, currentX + 20, currentY + lt/2)
                    
                    // 清理临时文件
                    tempFile.delete()
                    
                    // 绘制序号标签
                    val numberDetail = getNumberDetail(textFont, (i + 1).toString())
                    canvas.drawImage(numberDetail, currentX + imageDetail.width - 10f, currentY + imageDetail.height - 10f)
                    
                    // 在图片上方显示序号信息
                    val infoText = "序号: ${i + 1}"
                    val infoFont = Font(typeface, 16f)
                    val infoX = currentX + 15
                    val infoY = currentY
                    val textPaint = Paint().apply { 
                        color = Color.makeRGB(13, 13, 13)
                        isAntiAlias = true 
                    }
                    val infoDetail = getInfo(infoFont, infoText, backgroundPaint, textPaint)
                    canvas.drawImage(infoDetail, infoX, infoY)
                    
                } else {
                    // 如果data为空，尝试从文件系统加载
                    val filePath = "${imageFile.url}/${imageFile.md5}.${imageFile.type}"
                    val file = PluginMain.resolveDataFile(filePath)
                    
                    if (file.exists()) {
                        // 绘制统一大小的图片详情
                        val imageDetail = drawImageDetail(backgroundPaint, file)
                        canvas.drawImage(imageDetail, currentX + 20, currentY + lt/2)
                        
                        // 绘制序号标签
                        val numberDetail = getNumberDetail(textFont, (i + 1).toString())
                        canvas.drawImage(numberDetail, currentX + imageDetail.width - 10f, currentY + imageDetail.height - 10f)
                        
                        // 在图片上方显示序号信息
                        val infoText = "序号: ${i + 1}"
                        val infoFont = Font(typeface, 16f)
                        val infoX = currentX + 15
                        val infoY = currentY
                        val textPaint = Paint().apply { 
                            color = Color.makeRGB(13, 13, 13)
                            isAntiAlias = true 
                        }
                        val infoDetail = getInfo(infoFont, infoText, backgroundPaint, textPaint)
                        canvas.drawImage(infoDetail, infoX, infoY)
                    } else {
                        // 文件不存在时显示错误信息
                        val errorDetail = drawErrorDetail(targetSize, "文件缺失")
                        canvas.drawImage(errorDetail, currentX + 20, currentY + lt/2)
                    }
                }
                
                // 更新位置，与ImagePreviewDrawer保持一致
                currentX += targetSize + lt
                rowCount++
                if (rowCount >= numImagesPerRow) {
                    currentX = lt
                    currentY += targetSize + lt
                    rowCount = 0
                }
            } catch (e: SFException) {
                val errorDetail = drawErrorDetail(targetSize, "缩放失败")
                canvas.drawImage(errorDetail, currentX + 20, currentY + lt/2)

                // 更新位置
                currentX += targetSize + lt
                rowCount++
                if (rowCount >= numImagesPerRow) {
                    currentX = lt
                    currentY += targetSize + lt
                    rowCount = 0
                }

            } catch (e: Exception) {
                val errorDetail = drawErrorDetail(targetSize, "加载失败")
                canvas.drawImage(errorDetail, currentX + 20, currentY + lt/2)

                // 更新位置
                currentX += targetSize + lt
                rowCount++
                if (rowCount >= numImagesPerRow) {
                    currentX = lt
                    currentY += targetSize + lt
                    rowCount = 0
                }
            }
        }
    }
    
    /**
     * 绘制统一大小的图片详情，使用与ImagePreviewDrawer完全一致的实现
     */
    private fun drawImageDetail(backgroundPaint: Paint, originalImage: File): Image {
        val surfaceBitmap2 = Surface.makeRasterN32Premul(targetSize.toInt(), targetSize.toInt())

        val canvas3 = surfaceBitmap2.canvas
        val rect = RRect.makeXYWH(5f, 5f, targetSize - 5, targetSize - 5, 20f)

        val paint = Paint().apply {
            color = Color.makeRGB(20, 20, 20)
            strokeWidth = 1f
            strokeJoin = PaintStrokeJoin.ROUND
            mode = PaintMode.STROKE
            imageFilter = ImageFilter.makeBlur(1f, 1f, FilterTileMode.MIRROR)
        }
        canvas3.drawRRect(rect, backgroundPaint)
        canvas3.drawPath(
            Path().apply {
                addRRect(rect)
            }, paint
        )

        var image: Image = Image.makeFromEncoded(originalImage.readBytes())
        try {
            image =  getIMage(originalImage.readBytes(), image.width, image.height);
        }catch (e:Exception){
            throw SFException()
        }
        canvas3.drawImageRect(
            image,
            Rect.makeXYWH(15f, 25f, targetSize - 30f, targetSize - 30f)
        )

        return surfaceBitmap2.makeImageSnapshot()
    }

    /**
     * 使用与ImagePreviewDrawer完全一致的图片缩放逻辑
     */
    private fun getIMage(image: ByteArray, width: Int, height: Int): Image {
        val surfaceBitmap = Surface.makeRasterN32Premul(infoHeight, infoHeight)
        val avatarImage = Image.makeFromEncoded(image)
        val canvas2 = surfaceBitmap.canvas
        // 计算图像的缩放比例
        val scaleX = width / infoHeight
        val scaleY = height / infoHeight
        val scale = Integer.min(scaleX, scaleY)

        // 计算图像的偏移量
        val offsetX = (width / scale)
        val offsetY = (height / scale)
        val value = Integer.max(offsetX, offsetY)
        canvas2.drawImageRect(
            avatarImage,
            RRect.makeXYWH(0f, 0f, value.toFloat(), infoHeight.toFloat(), 10f),
            Paint()
        )
        return surfaceBitmap.makeImageSnapshot()
    }
    
    /**
     * 绘制信息标签，与ImagePreviewDrawer保持一致
     */
    private fun getInfo(font: Font, info: String, backgroundPaint: Paint, textPaint: Paint): Image {
        val tarwidth = font.measureText(info).width + 40f
        val tarheight = font.measureText(info).height + 10f
        val surfaceBitmap2 = Surface.makeRasterN32Premul((targetSize + 20).toInt(), tarheight.toInt() + 20)
        val canvas3 = surfaceBitmap2.canvas
        val rect = RRect.makeXYWH(5f, 5f, targetSize + 5, tarheight, 10f)

        val paint = Paint().apply {
            color = Color.makeRGB(20, 20, 20)
            strokeWidth = 1f
            strokeJoin = PaintStrokeJoin.ROUND
            mode = PaintMode.STROKE
            imageFilter = ImageFilter.makeBlur(1f, 1f, FilterTileMode.MIRROR)
        }
        canvas3.drawRRect(rect, backgroundPaint)
        canvas3.drawPath(
            Path().apply {
                addRRect(rect)
            }, paint
        )
        canvas3.drawString(info, targetSize / 2 - tarwidth / 2 + 20, 27f, font, textPaint)
        val path = Path().apply {
            RRect.makeXYWH(0f, 0f, targetSize, tarheight, 10f)
        }
        canvas3.clipPath(path)

        return surfaceBitmap2.makeImageSnapshot()
    }
    
    /**
     * 绘制序号标签，类似ImagePreviewDrawer的实现
     */
    private fun getNumberDetail(font: Font, number: String): Image {
        val size = 30
        val surface = Surface.makeRasterN32Premul(size, size)
        val canvas = surface.canvas
        
        val circlePaint = Paint().apply {
            color = Color.makeRGB(242, 80, 66)
            mode = PaintMode.FILL
        }
        
        // 绘制圆形背景
        canvas.drawCircle(15f, 15f, 15f, circlePaint)
        
        // 绘制数字
        val textPaint = Paint().apply { color = Color.WHITE }
        val textWidth = font.measureText(number).width
        val textX = if (number == "1") 10f else 15f - textWidth / 2f
        canvas.drawString(number, textX, 22f, font, textPaint)
        
        return surface.makeImageSnapshot()
    }
    
    /**
     * 绘制错误信息
     */
    private fun drawErrorDetail(targetSize: Float, errorText: String): Image {
        val surface = Surface.makeRasterN32Premul(targetSize.toInt(), targetSize.toInt())
        val canvas = surface.canvas
        
        // 绘制背景
        val backgroundPaint = Paint().apply { 
            color = Color.makeRGB(220, 220, 220)
            isAntiAlias = true
        }
        val rect = RRect.makeXYWH(5f, 5f, targetSize - 10f, targetSize - 10f, 20f)
        canvas.drawRRect(rect, backgroundPaint)
        
        // 绘制错误文字
        val textPaint = Paint().apply { 
            color = Color.makeRGB(100, 100, 100)
            isAntiAlias = true 
        }
        val font = Font(Typeface.makeFromName("微软雅黑", FontStyle.NORMAL), 16f)
        val textWidth = font.measureText(errorText).width
        val textX = (targetSize - textWidth) / 2
        val textY = targetSize / 2
        canvas.drawString(errorText, textX, textY, font, textPaint)
        
        return surface.makeImageSnapshot()
    }
}
class SFException: Exception("缩放失败")