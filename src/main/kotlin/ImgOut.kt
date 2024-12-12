package org.huvz.mirai.plugin

import org.jetbrains.annotations.TestOnly
import org.huvz.mirai.plugin.entity.GroupDetail
import org.huvz.mirai.plugin.entity.ImageFile
import util.skia.ImageDrawerComposer
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
@TestOnly
fun main() {
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
    println("MD5 of $file is: $md5")
    }
fun getMD5(bytes: ByteArray): String {
    val md = MessageDigest.getInstance("MD5")
    md.update(bytes)
    val digest = md.digest()
    return BigInteger(1, digest).toString(16).padStart(32, '0')
}