package org.huvz.mirai.plugin

import org.jetbrains.annotations.TestOnly
import org.huvz.mirai.plugin.entity.GroupDetail
import org.huvz.mirai.plugin.entity.ImageFile
import org.huvz.mirai.plugin.util.skia.GalleryDetailComposer
import util.skia.ImageDrawerComposer
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest

@TestOnly
fun main() {
//    // 测试原有的ImageDrawerComposer
//    testImageDrawerComposer()
    
    // 测试新的GalleryDetailComposer
    testGalleryDetailComposer()
}

@TestOnly
fun testImageDrawerComposer() {
    val filePath = "C:\\re\\va.jpg"
    val s = File(filePath)
    val groupDetail = GroupDetail(
        1114514.toString(),
        s.readBytes(),
        "test",
        "0",
        10,
        14
        );
    val newMap:HashMap<String,List<ImageFile>> = hashMapOf()
    val v1 = ImageFile();
    v1.type="jpg";v1.md5="cjj1";v1.url="C:\\re\\114514\\test\\22.jpg"
    v1.about="11";
    var v2 = ImageFile();
    v2.type="jpg";v2.md5="zms";v2.url="C:\\re\\114514\\11\\11.jpg"
    v2.about="11";
    newMap.put("11",arrayListOf(v1))
    newMap.put("22",arrayListOf(v2,v1,v1))
    newMap.put("233",arrayListOf(v2,v1,v1))
    newMap.put("44",arrayListOf(v2,v1,v1))
    newMap.put("55",arrayListOf(v2,v1,v1))
    newMap.put("66",arrayListOf(v2,v1,v1))
    newMap.put("77",arrayListOf(v2,v1,v1))
    val composer = ImageDrawerComposer(
        1430, (newMap.size/6+1)*(185+40)+200,
        "titleText", newMap, 6,
        groupDetail,
        40f,
        100,
        185f
    )
    val outputFile = composer.draw()
    val localDirectoryPath = "C:\\re"
    val localFilePath = "$localDirectoryPath/outimg.png"
    var fiile = outputFile.inputStream().use { inputStream ->
        File(localFilePath).outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }
    val file = File(localFilePath)
    val md5 = getMD5(file.readBytes())
    println("ImageDrawerComposer - MD5 of $file is: $md5")
}

@TestOnly
fun testGalleryDetailComposer() {
    println("开始测试 GalleryDetailComposer...")
    
    // 创建简单的测试数据，不依赖外部文件
    val groupDetail = GroupDetail(
        "1114514",
        ByteArray(0), // 空的头像数据
        "测试图库",
        "0",
        10,
        14
    )
    
    // 创建测试图片列表
    val imageList = mutableListOf<ImageFile>()
    // 使用一个1x1像素的有效PNG图片数据
    val validImageData = byteArrayOf(
        -119, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82, 0, 0, 0, 1, 0, 0, 0, 1, 8, 2, 0, 0, 0, -112, 119, -8, 36, 0, 0, 0, 12, 73, 68, 65, 84, 120, -100, 99, 96, 96, 96, 0, 0, 0, 4, 0, 1, -54, -53, -52, -50, 0, 0, 0, 0, 73, 69, 78, 68, -82, 66, 96, -126
    )
    
    for (i in 1..12) {
        val imageFile = ImageFile()
        imageFile.type = "png"
        imageFile.md5 = "test$i"
        imageFile.url = "test_path"
        imageFile.about = "测试图片$i"
        imageFile.data = validImageData // 添加有效的图片数据
        imageList.add(imageFile)
    }
    
    // 计算输出高度，与GalleryDetailDrawer保持一致
    val rows = (imageList.size + 6 - 1) / 6
    val outputHeight = 100 + 40 + rows * (185f + 40f + 40f) + 40
    
    try {
        val galleryComposer = GalleryDetailComposer(
            outputWidth = 1430,
            outputHeight = outputHeight.toInt(),
            titleText = "图库详情背景",
            galleryName = "测试图库",
            imageList = imageList,
            lt = 40f,
            infoHeight = 100,
            targetSize = 185f
        )
        
        println("正在渲染图片...")
        val outputFile = galleryComposer.draw()
        
        // 创建输出目录
        val localDirectoryPath = "C:\\temp"
        File(localDirectoryPath).mkdirs()
        val localFilePath = "$localDirectoryPath/gallery_detail_composed.png"
        
        outputFile.inputStream().use { inputStream ->
            File(localFilePath).outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        
        val file = File(localFilePath)
        val md5 = getMD5(file.readBytes())
        println("GalleryDetailComposer - MD5 of $file is: $md5")
        println("GalleryDetailComposer output saved to: $localFilePath")
        println("GalleryDetailComposer 测试成功！")
        
    } catch (e: Exception) {
        println("GalleryDetailComposer 测试失败: ${e.message}")
        e.printStackTrace()
    }
}

fun getMD5(bytes: ByteArray): String {
    val md = MessageDigest.getInstance("MD5")
    md.update(bytes)
    val digest = md.digest()
    return BigInteger(1, digest).toString(16).padStart(32, '0')
}