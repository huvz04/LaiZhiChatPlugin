package org.huvz.mirai.plugin.util.skia

import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Surface
import org.jetbrains.skia.Data
import org.jetbrains.skia.Typeface
import org.huvz.mirai.plugin.entity.GroupDetail
import org.huvz.mirai.plugin.entity.ImageFile
import org.huvz.mirai.plugin.util.skia.impl.GroupInfoDrawer
import org.huvz.mirai.plugin.util.skia.impl.GalleryDetailDrawer
import util.skia.impl.BackgroundDrawer
import util.skia.impl.MaskDrawer
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class GalleryDetailComposer(
    private val outputWidth: Int,
    private val outputHeight: Int,
    private val titleText: String,
    private val galleryName: String,
    private val imageList: List<ImageFile>,
//    private val groupDetail: GroupDetail,
    private val lt: Float,
    private val infoHeight: Int,
    private val targetSize: Float
) {

    fun draw(): File {
        val surface = Surface.makeRasterN32Premul(outputWidth, outputHeight)
        val canvas = surface.canvas

        val drawers = listOf(
            BackgroundDrawer(outputWidth, outputHeight, titleText),
            MaskDrawer(outputWidth, outputHeight, infoHeight, lt),
            GalleryDetailDrawer(
                galleryName = galleryName,
                imageList = imageList,
                outputWidth = outputWidth,
                numImagesPerRow = 6,
                infoHeight = infoHeight,
                lt = lt,
                targetSize = targetSize
            ),
            //GroupInfoDrawer(1, outputWidth, groupDetail, infoHeight, lt) // 图库数量设为1，因为这是单个图库详情
        )

        // 加载字体，与ImageDrawerComposer保持一致
        val fontStream: InputStream = this.javaClass.classLoader.getResourceAsStream("MiSans-Demibold.ttf")
        val fontData = Data.makeFromBytes(fontStream.readBytes())
        val typeface = Typeface.makeFromData(fontData)

        drawers.forEach { drawer ->
            drawer.fontTypeface = typeface
            drawer.draw(canvas)
        }

        val tempFile = File.createTempFile("gallery_detail_composed", ".png")
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
}