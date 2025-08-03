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
import java.io.File
import java.nio.file.Paths
import java.util.*
import kotlin.random.Random


class ImageUtils: Closeable {
    companion object
    {
        // 用于存储每个群组的已使用索引，避免重复
        private val usedIndicesMap = mutableMapOf<String, MutableSet<Int>>()
        
        fun GetImage(group: Group, folderpath:String, picnum:Int ): ExternalResource? {

            try {
                val path = Paths.get("data", "org.huvz.laizhi", "LaiZhi", group.id.toString(), folderpath)
//                val filepath = File("data\\org.huvz.laizhi\\LaiZhi\\${group.id}\\${folderpath}")
                val filepath = File(path.toUri());
//                logger.info { "开始获取文件夹:${filepath.absolutePath}" }
                val images = filepath.listFiles { file -> file.extension == "jpg" || file.extension == "png" || file.extension == "gif"}
                if (images != null && images.isNotEmpty()) {
                    val rad = if(picnum!=-1){
                        picnum-1
                    } else {
                        Random.nextInt(0, images.size)
                    }

                    val randomImage = images[rad]
//                    logger.info { "本地已找到${randomImage.absolutePath}" }
                    val res = randomImage.toExternalResource().toAutoCloseable()
                    return res;
                }else{
                    logger.error("获取失败${filepath.absolutePath}")
                }

            }catch (e:Exception){
                logger.error( "获取图片异常: ${e.message}")
            }
            return null
        }
        
        /**
         * 获取多张不重复的随机图片索引
         */
        fun getRandomIndices(totalCount: Int, requestCount: Int, groupId: String, galleryName: String): List<Int> {
            if (requestCount >= totalCount) {
                // 如果请求数量大于等于总数量，返回所有索引
                return (0 until totalCount).toList()
            }
            
            val key = "${groupId}_${galleryName}"
            val usedIndices = usedIndicesMap.getOrPut(key) { mutableSetOf() }
            
            // 如果已使用的索引数量加上请求数量超过总数量，清空已使用索引
            if (usedIndices.size + requestCount > totalCount) {
                usedIndices.clear()
            }
            
            val availableIndices = (0 until totalCount).filter { it !in usedIndices }
            val selectedIndices = availableIndices.shuffled().take(requestCount)
            
            // 将选中的索引添加到已使用集合中
            usedIndices.addAll(selectedIndices)
            
            return selectedIndices
        }
        
        /**
         * 清空指定群组和图库的已使用索引缓存
         */
        fun clearUsedIndices(groupId: String, galleryName: String) {
            val key = "${groupId}_${galleryName}"
            usedIndicesMap.remove(key)
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
            val filePath = "LaiZhi\\${group.id}\\${from}\\${image.imageId}"
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