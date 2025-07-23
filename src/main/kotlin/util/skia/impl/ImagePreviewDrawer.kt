package util.skia.impl

import org.jetbrains.skia.*
import org.huvz.mirai.plugin.PluginMain
import org.huvz.mirai.plugin.entity.ImageFile
import org.huvz.mirai.plugin.util.skia.ImageDrawer
import java.io.File


class ImagePreviewDrawer(
    private val fileList: Map<String,List<ImageFile>>,
    private val outputWidth: Int,
    private val outputHeight: Int,
    private val numImagesPerRow: Int,
    private val infoHeight:Int,
    private val lt:Float,
    private val targetSize:Float

) : ImageDrawer {
    override var fontTypeface: Typeface? = null

    override fun draw(canvas: Canvas) {

        val font =
            if(fontTypeface!=null){
                Font(fontTypeface, 20f)
            }
            else {
                Font(Typeface.makeFromName("MiSans",FontStyle.BOLD), 20f) }
        val detailPaint = Paint().apply { color = Color.makeRGB(173, 216, 230) }
        val webPaint = Paint().apply { color = Color.makeRGB(240, 248, 255) }
        val ccPaint = Paint().apply { color = Color.makeRGB(242, 80, 66) }
        val textPaint = Paint().apply {
            color = Color.makeRGB(13, 13, 13)
            isAntiAlias = true
        }
        var currentX = lt
        var currentY = infoHeight.toFloat() + lt * 2
        var rowCount = 0

        for ((_, fileList) in fileList) {
            val randoms = fileList.indices.random()
            val chs = fileList[randoms];
            val file = PluginMain.resolveDataFile(chs.url+"/${chs.md5}.${chs.type}")
            try {
                val imageDetail = drawImageDetail(webPaint, file)
                canvas.drawImage(imageDetail, currentX+20, currentY+lt/2)

                val infoName = getInfo(font, chs.about, detailPaint, textPaint)
                canvas.drawImage(infoName, currentX +15, currentY )

                val infoNum = getNumberDetail(font,fileList.size.toString())
                canvas.drawImage(infoNum,currentX+imageDetail.width-10f,currentY+imageDetail.height-10f)
                currentX += targetSize + lt
                rowCount++
                if (rowCount >= numImagesPerRow) {
                    currentX = lt
                    currentY += targetSize + lt
                    rowCount = 0
                }
            } catch (e: Exception) {
                println("Error reading image file: $e")
            }
        }
    }
    private fun drawImageDetail(
        backgroundPaint: Paint,
        originalImage:File,
    ): Image {
        val surfaceBitmap2 = Surface.makeRasterN32Premul(targetSize.toInt(), targetSize.toInt())

        val canvas3 = surfaceBitmap2.canvas
        val rect = RRect.makeXYWH(5f,5f,targetSize-5,
            targetSize-5,20f)


        val paint = Paint().apply {
            color = Color.makeRGB(20,20,20)
            strokeWidth = 1f
            strokeJoin = PaintStrokeJoin.ROUND
            mode = PaintMode.STROKE
            imageFilter = ImageFilter.makeBlur(1f,1f, FilterTileMode.MIRROR)
        }
        canvas3.drawRRect(rect,backgroundPaint)
//        canvas3.clipPath(rectdetailPath)
        canvas3.drawPath(
            Path().apply {
                addRRect(rect)
            }
            ,paint)

        var image :Image = Image.makeFromEncoded(originalImage.readBytes())

        canvas3.drawImageRect(
            getIMage(originalImage.readBytes(),image.width,image.height),
            Rect.makeXYWH(15f, 25f, targetSize-30f, targetSize-30f)
        )


        return surfaceBitmap2.makeImageSnapshot()

    }
    private fun getIMage(image:ByteArray,width:Int,height:Int): Image {
        val surfaceBitmap = Surface.makeRasterN32Premul(infoHeight, infoHeight)
        val avatarImage = Image.makeFromEncoded(image)
        val canvas2 = surfaceBitmap.canvas
        // 计算图像的缩放比例
        val scaleX =  width / infoHeight
        val scaleY = height / infoHeight
        val scale = Integer.min(scaleX, scaleY)

        // 计算图像的偏移量
        val offsetX = (width / scale)
        val offsetY = (height / scale)
        val value = Integer.max(offsetX,offsetY)
        canvas2.drawImageRect(
            avatarImage,
            RRect.makeXYWH(0f, 0f, value.toFloat(), infoHeight.toFloat(),10f)
            ,Paint())
//        val path = Path().apply {
//            RRect.makeXYWH(0f, 0f, value.toFloat(), .toFloat(),10f)
//        }
//        canvas2.clipPath(path)
        return surfaceBitmap.makeImageSnapshot()
    }

    private fun getInfo(font:Font, info:String, backgroundPaint:Paint,textPaint: Paint):Image{

        val tarwidth = font.measureText(info).width+40f
        val tarheight = font.measureText(info).height+10f
        val surfaceBitmap2 = Surface.makeRasterN32Premul((targetSize+20).toInt(), tarheight.toInt()+20)
        val canvas3 = surfaceBitmap2.canvas
        val rect = RRect.makeXYWH(5f,5f,targetSize+5,tarheight,10f)


        val paint = Paint().apply {
            color = Color.makeRGB(20,20,20)
            strokeWidth = 1f // 设置画笔宽度为 3 像素
            strokeJoin = PaintStrokeJoin.ROUND
            mode = PaintMode.STROKE
            imageFilter = ImageFilter.makeBlur(1f,1f,FilterTileMode.MIRROR)
        }
        canvas3.drawRRect(rect,backgroundPaint)
//        canvas3.clipPath(rectdetailPath)
        canvas3.drawPath(
            Path().apply {
                addRRect(rect)
            }
            ,paint)
        canvas3.drawString(info, targetSize/2-tarwidth/2+20, 27f, font, textPaint)
        val path = Path().apply {
            RRect.makeXYWH(0f, 0f, targetSize,tarheight,10f)
        }
        canvas3.clipPath(path)

        return surfaceBitmap2.makeImageSnapshot()

    }

    private fun getNumberDetail(font:Font,number:String):Image{
        val surfaceBitmap2 = Surface.makeRasterN32Premul(infoHeight, infoHeight)
        val canvas3 = surfaceBitmap2.canvas
        val circlePaint = Paint().apply {
            color = Color.makeRGB(242,80,66)
            mode = PaintMode.FILL
        }
        //绘制圆
        canvas3.drawCircle(15f,15f,15f,circlePaint)
        val textCirclePaint = Paint().apply {
            color = Color.WHITE
            Paint
        }
        val textWith1 = font.measureText(number)
        if(number=="1") canvas3.drawString(number, 10f, 22f, font,textCirclePaint)
        else canvas3.drawString(number, 15f-(textWith1.width/2f), 22f, font,textCirclePaint)
        return surfaceBitmap2.makeImageSnapshot()
    }
}