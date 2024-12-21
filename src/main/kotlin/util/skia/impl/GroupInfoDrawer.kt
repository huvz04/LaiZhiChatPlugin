package org.huvz.mirai.plugin.util.skia.impl

import org.jetbrains.skia.*
import org.huvz.mirai.plugin.entity.GroupDetail
import org.huvz.mirai.plugin.util.skia.ImageDrawer
import java.io.InputStream

class GroupInfoDrawer
    (
    private val groupNum:Int,
    private val outputWidth: Int,
    private val groupDetail: GroupDetail,
    private val infoHeight:Int,
    private val lt: Float
        )
    : ImageDrawer {
    override var fontTypeface: Typeface? = null;

    override fun draw(canvas: Canvas) {
        // 绘制群头像

        val surfaceBitmap = Surface.makeRasterN32Premul(infoHeight, infoHeight)
        val fontStream: InputStream = this.javaClass.classLoader.getResourceAsStream("MiSans-Medium.ttf")
        val fontData = Data.makeFromBytes(fontStream.readBytes());
        val Mediumtypeface = Typeface.makeFromData(fontData)

        val surfaceBitmap2 = Surface.makeRasterN32Premul(infoHeight, infoHeight)
        val fontStream2: InputStream = this.javaClass.classLoader.getResourceAsStream("MiSans-Light.ttf")
        val fontData2 = Data.makeFromBytes(fontStream.readBytes());
        val Lighttypeface = Typeface.makeFromData(fontData)
        val avatarRadius = infoHeight / 2f - 10f
        val avatarX = lt + 10f
        val avatarY = infoHeight / 2f - 5f
        val avatarImage = Image.makeFromEncoded(groupDetail.avatar)


        val canvas2 = surfaceBitmap.canvas
        val rectPath = Path().apply {
            addCircle(avatarX, avatarY, avatarRadius,PathDirection.CLOCKWISE)
        }
        canvas2.clipPath(rectPath)
        canvas2.drawImageRect(
            avatarImage,
            Rect.makeXYWH(0f, 0f, infoHeight.toFloat(), infoHeight.toFloat())
            ,Paint())
        val imageavatar = surfaceBitmap.makeImageSnapshot()
        canvas.drawImage(imageavatar,avatarX,avatarY)


        // 绘制群名称
        val nameX = avatarX + avatarRadius + 25f + lt
        val nameY = avatarY - 5f + lt
        val TitleFont = Font(fontTypeface, 24f)
        val TitlePaint = Paint().apply {
            color = Color.BLACK
        }
        canvas.drawString(groupDetail.name, nameX, nameY,TitleFont,TitlePaint)
        val infoPaint = Paint().apply {
            color = Color.makeRGB(139,139,156)
        }
        val idtextFont = Font(Lighttypeface, 20f)
        // 群号

        canvas.drawString(" ${groupDetail.id}", nameX, nameY + 35f,idtextFont, infoPaint)
        val infoText = " 图片 | ${groupDetail.total} "
        // 计算 =  图片坐标x+ 半径 + 标题长度    + lt 缓冲
        val infoX = avatarX + avatarRadius + TitleFont.measureTextWidth(groupDetail.name) + lt + 60f
//        val infoX = outputWidth  - idtextFont.measureText(infoText).width - 200f
        val infoY = nameY
        val detailtFont = Font(Mediumtypeface, 20f)

        val webpaint = Paint().apply {
            color = Color.makeRGB( 	255,142,60)
        }
        val textPaint = Paint().apply {
//            color = Color.BLACK
            color = Color.makeRGB(13,13,13)
            isAntiAlias = true
        }
        canvas.drawImage(getInfo(detailtFont,infoText,webpaint,textPaint),infoX,infoY)
        // 画廊数
        val infoX2 = infoX +  idtextFont.measureText(infoText).width + 32f
        val webpaint2 = Paint().apply {
            color = Color.makeRGB(106, 44, 112)
        }
        val infoText2 = " 图库 | ${groupNum} "
        val textPaint2 = Paint().apply {
            color = Color.makeRGB(250,244,250)
            isAntiAlias = true
        }
        canvas.drawImage(getInfo(detailtFont,infoText2,webpaint2,textPaint2),infoX2,infoY)



        val infoX3 = infoX2 +  idtextFont.measureText(infoText2).width + 32f
        val webpaint3 = Paint().apply {
            color = Color.makeRGB(7,166,128)
        }
        val infoText3 = " 群成员 | ${groupDetail.galleryNumber} "
        val textPaint3 = Paint().apply {
            color = Color.makeRGB(13,13,13)
            isAntiAlias = true
        }
        canvas.drawImage(getInfo(detailtFont,infoText3,webpaint3,textPaint3),infoX3,infoY)

    }


    private fun getInfo(font:Font, info:String, backgroundPaint:Paint,textPaint: Paint):Image{
        val surfaceBitmap2 = Surface.makeRasterN32Premul(infoHeight+90, infoHeight)

        val canvas3 = surfaceBitmap2.canvas
        val rect = RRect.makeXYWH(5f,5f,font.measureText(info).width+20f,font.measureText(info).height+10f,10f)

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
        canvas3.drawString(info, 10f, 27f, font, textPaint)


       return surfaceBitmap2.makeImageSnapshot()

    }
}