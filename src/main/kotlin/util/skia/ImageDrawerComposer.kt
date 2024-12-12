package util.skia

import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Surface
import org.huvz.mirai.plugin.entity.GroupDetail
import org.huvz.mirai.plugin.entity.ImageFile
import org.huvz.mirai.plugin.util.skia.impl.GroupInfoDrawer
import util.skia.impl.BackgroundDrawer
import util.skia.impl.ImagePreviewDrawer
import util.skia.impl.MaskDrawer
import java.io.File
import java.io.FileOutputStream

class ImageDrawerComposer(
    private val outputWidth: Int,
    private val outputHeight: Int,
    private val titleText: String,
    private val fileList: Map<String,List<ImageFile>>,
    private val numImagesPerRow: Int,
    private val groupDetail: GroupDetail,
    private val lt: Float,
    private val infoHeight:Int,
    private val targetSize:Float
) {

    fun draw(): File {
        val surface = Surface.makeRasterN32Premul(outputWidth, outputHeight)
        val canvas = surface.canvas

        val mainHeight = outputWidth / 2f - (outputWidth / 2 - lt)

        val drawers = listOf(
            BackgroundDrawer(outputWidth, outputHeight, titleText),
            MaskDrawer(outputWidth, outputHeight,infoHeight,lt),
            ImagePreviewDrawer(
                fileList,
                outputWidth,
                outputHeight,
                numImagesPerRow,
                infoHeight,
                lt,
                targetSize,
                ),

            GroupInfoDrawer(fileList.size,outputWidth,groupDetail,infoHeight,lt)
        )

        drawers.forEach { drawer ->
            drawer.draw(canvas)
        }

        val tempFile = File.createTempFile("image", ".png")
        val outputStream = FileOutputStream(tempFile)
        val image = surface.makeImageSnapshot()
        val encoded = image.encodeToData(EncodedImageFormat.PNG, 110)
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