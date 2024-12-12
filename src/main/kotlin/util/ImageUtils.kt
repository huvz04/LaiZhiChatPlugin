package org.huvz.mirai.plugin.util

import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.ExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.info
import okhttp3.Request
import org.huvz.mirai.plugin.PluginMain.logger
import org.huvz.mirai.plugin.PluginMain.resolveDataFile
import java.io.Closeable
import java.util.*
import kotlin.random.Random


class ImageUtils: Closeable {
    companion object
    {
        fun GetImage(group: Group, folderpath:String, picnum:Int ): ExternalResource? {

            try {
                val filepath = resolveDataFile("LaiZhi\\${group.id}\\${folderpath}")
                val images = filepath.listFiles { file -> file.extension == "jpg" || file.extension == "png" || file.extension == "gif"}
                if (images != null && images.isNotEmpty()) {
                    val rad = if(picnum!=-1){
                        picnum-1
                    } else {
                        Random.nextInt(0,images.size+1)%images.size;
                    }

                    val randomImage = images[rad]
                    logger.info { "本地已找到${randomImage.absolutePath}" }
                    val res = randomImage.toExternalResource().toAutoCloseable()
                    return res;
                }

            }catch (e:Exception){
                logger.error( "获取图片异常")
            }
            return null
        }



        /**
         * 保存图片到本地目录
         */
        suspend fun saveImage(group: Group,from: String, image: Image) {
            val url = image.queryUrl()
            val uniqueId = UUID.randomUUID().toString()
            var ParentfilePath = "LaiZhi\\${group.id}\\${from}"
            val fileParent  = resolveDataFile(ParentfilePath)
            if (!fileParent.exists()) fileParent.mkdirs()
            //logger.info( "初始化目录：${ParentfilePath}")
                val request = Request.Builder()
                    .url(url)
                    .build()
                val reponse =  HttpClient.okHttpClient.newCall(request).execute()
                val imageByte = reponse.body!!.bytes()
                if(imageByte.isEmpty()) throw Exception("没数据")
                val contentType = reponse.header("Content-Type")
                val fileType :String?;
                when (contentType) {
                    "image/jpeg" -> fileType = "jpg"
                    "image/png" -> fileType = "png"
                    "image/gif" -> fileType = "gif"
                    else -> fileType = "jpg"
                }
                val filePath = ParentfilePath +  "\\${uniqueId}.${fileType}"
                val file  = resolveDataFile(filePath)
                file.writeBytes(imageByte)
            logger.info( "写入文件：${filePath}")

        }

        /**
         * 从本地目录删除图片
         */
        public suspend fun delImages(group: Group,from: String, image: Image):Boolean {
            val url = image.queryUrl()
            val filePath = "LaiZhi/${group.id}\\${from}\\${image.imageId}"
            val file  = resolveDataFile(filePath)
            if(file.exists()){

                file.deleteRecursively()
                if(file.exists()) file.delete()
            }
                return !file.exists();


        }


    }
    override fun close() {
        this.close()
    }


}