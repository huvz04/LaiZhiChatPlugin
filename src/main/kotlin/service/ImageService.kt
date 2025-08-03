package org.huvz.mirai.plugin.Service

import entity.LZException
import entity.data.GroupDetails
import entity.data.GroupDetails.key
import entity.data.ImageFiles
import entity.data.ImageFiles.about
import entity.data.ImageFiles.qq
import net.mamoe.mirai.utils.ExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.huvz.mirai.plugin.PluginMain
import org.huvz.mirai.plugin.PluginMain.DataMP
import org.huvz.mirai.plugin.entity.GroupDetail
import org.huvz.mirai.plugin.entity.ImageFile
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import org.huvz.mirai.plugin.PluginMain.resolveDataFile
import util.IdWorker
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path

object ImageService {
    //    private val db = Database.connect(
//        "jdbc:postgresql://localhost:5432/postgres",
//        user = "postgres", password = "postgres"
//    )
    val dataPath = resolveDataFile("data.db").absolutePath;
    private val db = Database.connect(
        "jdbc:sqlite:${dataPath}",
        driver = "org.sqlite.JDBC"
    )

    fun initDatabase() {
        transaction {
            SchemaUtils.create(ImageFiles)
            SchemaUtils.create(GroupDetails)
        }
        // 启动时自动备份数据库
        backupDatabase()
    }
    
    /**
     * 备份数据库
     */
    fun backupDatabase() {
        try {
            val dbFile = PluginMain.resolveDataFile("LaiZhi.db")
            if (dbFile.exists()) {
                val timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                val backupFile = PluginMain.resolveDataFile("backup/LaiZhi_backup_$timestamp.db")
                backupFile.parentFile?.mkdirs()
                dbFile.copyTo(backupFile, overwrite = true)
                PluginMain.logger.info("数据库备份成功: ${backupFile.name}")
            }
        } catch (e: Exception) {
            PluginMain.logger.error("数据库备份失败: ${e.message}")
        }
    }

    /**
     * 获取群聊信息
     * @param id qq群id
     * @return 成功码
     */

//    fun selectGroupDetail(id: Int): GroupDetail {
//        return transaction(db) {
//            GroupDetails.selectAll().where { GroupDetails.id eq id.toString() }
//                .map {
//                    GroupDetail()
//                }
//                .firstOrNull() ?: throw IllegalArgumentException("Group with ID $id not found")
//        }
//    }


    /**
     * 更新并迁移图片
     *
     */
    suspend fun updateGrouplist(id: Long): Int {
        val ParentfilePath = "LaiZhi/$id"
        val filepath = PluginMain.resolveDataFile(ParentfilePath)
        val filelist = filepath.listFiles()
        filelist?.forEach {
            run {
                val filename = it.name
                //获取图片列表
                val foldlist =
                    File(filepath.absolutePath + "\\${filename}")
                        .listFiles { file ->
                            file.extension == "jpg" ||
                                file.extension == "png" ||
                                file.extension == "gif"
                        }
                //遍历flodlist 把每个图片都用fun getMD5(bytes: ByteArray): String计算一遍md5
                for (fold in foldlist!!) {
                    val md5b = getMD5(fold.readBytes())
                    transaction(db) {
                        ImageFiles.select(ImageFiles.md5)
                            .where { (ImageFiles.md5 eq md5b) }.firstOrNull()
                    }.let {
                        if (it == null) {
                            ImageFiles.select(ImageFiles.md5)
                            saveImage(id, filename, fold.readBytes(), getFileExtension(fold))
                            fold.deleteOnExit()
                        }
                    }

                }

            }
        }
        return 1
    }
    
    /**
     * 重建指定群聊的数据库
     * @param groupId 群聊ID，如果为null则重建所有群聊
     * @return 重建结果信息
     */
    suspend fun rebuildDatabase(groupId: Long? = null): String {
        return try {
            var totalProcessed = 0
            var totalAdded = 0
            
            if (groupId != null) {
                // 重建指定群聊
                val result = rebuildSingleGroup(groupId)
                totalProcessed = result.first
                totalAdded = result.second
                "群聊 $groupId 重建完成！扫描了 $totalProcessed 个文件，新增 $totalAdded 张图片到数据库"
            } else {
                // 重建所有群聊
                val laizhiDir = PluginMain.resolveDataFile("LaiZhi")
                if (!laizhiDir.exists()) {
                    return "LaiZhi目录不存在，无法重建数据库"
                }
                
                val groupDirs = laizhiDir.listFiles { file -> file.isDirectory && file.name.matches(Regex("\\d+")) }
                if (groupDirs.isNullOrEmpty()) {
                    return "未找到任何群聊目录"
                }
                
                for (groupDir in groupDirs) {
                    val gid = groupDir.name.toLongOrNull()
                    if (gid != null) {
                        val result = rebuildSingleGroup(gid)
                        totalProcessed += result.first
                        totalAdded += result.second
                    }
                }
                "全部群聊重建完成！总共扫描了 $totalProcessed 个文件，新增 $totalAdded 张图片到数据库"
            }
        } catch (e: Exception) {
            PluginMain.logger.error("重建数据库失败: ${e.message}")
            "重建数据库失败: ${e.message}"
        }
    }
    
    /**
     * 重建单个群聊的数据库
     */
    private suspend fun rebuildSingleGroup(groupId: Long): Pair<Int, Int> {
        var processedCount = 0
        var addedCount = 0
        
        val groupPath = PluginMain.resolveDataFile("LaiZhi/$groupId")
        if (!groupPath.exists()) {
            return Pair(0, 0)
        }
        
        val galleryDirs = groupPath.listFiles { file -> file.isDirectory }
        galleryDirs?.forEach { galleryDir ->
            val galleryName = galleryDir.name
            if (isPathSafe(galleryName)) {
                val imageFiles = galleryDir.listFiles { file ->
                    file.isFile && (file.extension.lowercase() in listOf("jpg", "jpeg", "png", "gif"))
                }
                
                imageFiles?.forEach { imageFile ->
                    try {
                        processedCount++
                        val md5 = getMD5(imageFile.readBytes())
                        
                        // 检查数据库中是否已存在
                        val exists = transaction(db) {
                            ImageFiles.selectAll()
                                .where { 
                                    (ImageFiles.md5 eq md5) and 
                                    (ImageFiles.qq eq groupId.toString()) and 
                                    (ImageFiles.about eq galleryName)
                                }
                                .firstOrNull() != null
                        }
                        
                        if (!exists) {
                            // 保存到数据库
                            saveImageFromFile(groupId, galleryName, imageFile)
                            addedCount++
                        }
                    } catch (e: Exception) {
                        PluginMain.logger.error("处理文件 ${imageFile.name} 失败: ${e.message}")
                    }
                }
            }
        }
        
        return Pair(processedCount, addedCount)
    }
    
    /**
     * 从本地文件保存图片信息到数据库
     */
    private suspend fun saveImageFromFile(groupId: Long, galleryName: String, imageFile: File) {
        val md5 = getMD5(imageFile.readBytes())
        val fileType = getFileExtension(imageFile).removePrefix(".")
        val relativePath = "LaiZhi/$groupId/$galleryName/"
        
        transaction(db) {
            ImageFiles.insert {
                it[id] = IdWorker().nextId()
                it[this.md5] = md5
                it[qq] = groupId.toString()
                it[count] = 0L
                it[about] = galleryName
                it[type] = fileType
                it[url] = relativePath
            }
        }
        
        // 更新内存映射
        updateMapAfterInsert(groupId.toString(), galleryName)
    }
    /**
     * 校验并清理数据库中的无效记录
     * 1. 检查图库名称是否安全
     * 2. 检查所有图片记录对应的物理文件是否存在
     * @return 删除的记录数量
     */
    fun deleteUnsafeFiles(): Int {
        var deletedCount = 0
        return transaction {
            // 检查图库名称是否安全
            ImageFiles.selectAll().where { ImageFiles.about.like("%/%") }.forEach { result ->
                val aboutValue = result[ImageFiles.about]
                if (!isPathSafe(aboutValue)) {
                    ImageFiles.deleteWhere { ImageFiles.about eq aboutValue }
                    deletedCount++
                    PluginMain.logger.info("删除不安全的图库名称记录: $aboutValue")
                }
            }
            
            // 检查所有图片记录对应的物理文件是否存在
            ImageFiles.selectAll().forEach { result ->
                val md5Value = result[ImageFiles.md5]
                val qqValue = result[ImageFiles.qq]
                val aboutValue = result[ImageFiles.about]
                val typeValue = result[ImageFiles.type]
                
                val filePath = "LaiZhi/$qqValue/$aboutValue/$md5Value.$typeValue"
                val file = resolveDataFile(filePath)
                
                if (!file.exists() || file.length() == 0L) {
                    // 物理文件不存在或为空文件，删除数据库记录
                    val id = result[ImageFiles.id]
                    ImageFiles.deleteWhere { ImageFiles.id eq id }
                    deletedCount++
                    PluginMain.logger.info("删除无效图片记录: $filePath")
                }
            }
            
            // 更新内存中的数据映射
            if (deletedCount > 0) {
                PluginMain.DataMP = queryDataToMap()
            }
            
            deletedCount // 返回删除的数据量
        }
    }
    /**
     * 获取文件后缀
     *
     * @param file 要获取文件后缀的文件
     * @return 文件后缀
     */
    fun getFileExtension(file: File?): String {
        var extension = ""
        if (file != null && file.exists()) {
            val name = file.name
            extension = name.substring(name.lastIndexOf("."))
        }
        return extension
    }

    /**
     * 获取群聊的图片列表
     */
    fun selectImageDetail(id: Long): List<ImageFile> {
        return transaction(db) {
            ImageFiles.selectAll()
                .where { ImageFiles.qq eq id.toString() }
                .map {
                    ImageFile(
                        it[ImageFiles.id] ?: 0, // 处理 id 为 null 的情况
                        it[ImageFiles.md5] ?: "", // 处理 md5 为 null 的情况
                        it[ImageFiles.qq] ?: "", // 处理 qq 为 null 的情况
                        it[ImageFiles.count] ?: 0, // 处理 count 为 null 的情况
                        it[ImageFiles.about] ?: "", // 处理 about 为 null 的情况
                        it[ImageFiles.type] ?: "", // 处理 type 为 null 的情况
                        it[ImageFiles.url] ?: "" // 处理 url 为 null 的情况
                    )
                }
        }
    }

    /**
     * 更新qq群图库信息
     */
//    fun updateGroupDetail(qq: Long) {
//        transaction(db) {
//            val entity = selectImageDetail(qq)
//            val entity2 = selectGroupDetail(qq.toInt())
//            entity2.total = entity.size
////            GroupDetails.update({ GroupDetails.id eq entity2.id }) {
////                it[total] = entity2.total
////            }
//        }
//    }

    /**
     * 获取图片
     */
    suspend fun getImage(q1: Long, name: String): ExternalResource {
        return transaction(db) {
            ImageFiles.selectAll()
                .where { (ImageFiles.qq eq q1.toString()) and (ImageFiles.about eq name) }
                .map {
                    ImageFile(
                        0,
                        it[ImageFiles.md5],
                        it[ImageFiles.qq],
                        it[ImageFiles.count],
                        it[ImageFiles.about],
                        it[ImageFiles.type],
                        it[ImageFiles.url]
                    )
                }
                .randomOrNull() ?: throw IllegalArgumentException("No image found for group $q1 and name $name")
        }.let {
//            PluginMain.logger.info("获取到图片${it.md5},随机数")
            val ParentfilePath = "LaiZhi/$q1/$name/${it.md5}.${it.type}"
            val file = PluginMain.resolveDataFile(ParentfilePath)
            file.toExternalResource().toAutoCloseable()
        }
    }

    suspend fun getRandomImage(q1: Long): ExternalResource {
        return transaction(db) {
            ImageFiles.selectAll()
                .where { (ImageFiles.qq eq q1.toString()) }
                .map {
                    ImageFile(
                        0,
                        it[ImageFiles.md5],
                        it[ImageFiles.qq],
                        it[ImageFiles.count],
                        it[ImageFiles.about],
                        it[ImageFiles.type],
                        it[ImageFiles.url]
                    )
                }
                .randomOrNull()
        }.let {
            val ParentfilePath = "LaiZhi/$q1/${it?.about}/${it?.md5}.${it?.type}"
            val file = PluginMain.resolveDataFile(ParentfilePath)
            file.toExternalResource().toAutoCloseable()
        }
    }

    /**
     * 保存图片信息
     */
    suspend fun saveImage(q1: Long, name: String, imageByte: ByteArray, fileType: String) {
        val ParentfilePath = "LaiZhi/$q1/$name/"
        val fileParent = resolveDataFile(ParentfilePath)
        if (!fileParent.exists()) fileParent.mkdirs()

        val md5a = getMD5(imageByte)
        val filePath = File(fileParent, "$md5a.$fileType")
//        filePath.writeBytes(imageByte)
        val file = resolveDataFile(filePath.absolutePath)
        file.writeBytes(imageByte)
        transaction(db) {
            val entity = ImageFiles.selectAll()
                .where {
                    (ImageFiles.md5 eq md5a.toString()) and
                        (ImageFiles.qq eq q1.toString()) and
                        (ImageFiles.about eq name.toString())
                }
                .firstOrNull()
            if (entity != null) throw LZException("该图库已存在相同的图片")
            ImageFiles.insert {
                it[id] = IdWorker().nextId()
                it[md5] = md5a.toString()
                it[qq] = q1.toString()
                it[count] = 0L
                it[about] = name
                it[type] = fileType
                it[url] = ParentfilePath
            }
            updateMapAfterInsert( q1.toString(), name)
        }
    }

    /**
     * 更新图片信息
     */
    fun updateImage(imageFile: ImageFile) {
        transaction(db) {
            ImageFiles.update(
                {
                    ImageFiles.id eq 8
                }
            ) {
                it[md5] = imageFile.md5
                it[qq] = imageFile.qq
                it[count] = imageFile.count
                it[about] = imageFile.about
                it[type] = imageFile.type
                it[url] = imageFile.url
            }
        }
    }
    /**
     * 检查图库是否存在
     */
    fun checkImage(name: String,qq1:String) :Long{
        return transaction(db) {
            ImageFiles.selectAll().where {
                (about eq name) and (qq eq qq1)
            }.count()
        }

    }
    fun updateMapByClear(qqid: String, name: String) {
        synchronized(DataMP) {
            if (DataMP.containsKey(qqid)) {
                DataMP[qqid]!!.remove(name)
            }
        }
    }
    /**
     * 清理整个图库
     */
    fun clearImage(name: String,qq1:String) :Int{
        return transaction(db) {
            updateMapByClear(qq1,name);
            ImageFiles.deleteWhere {
                (about eq name) and (qq eq qq1)
            }

        }

    }
    /**
     * 删除图片信息
     */
    fun deleteImage(id: Long) {
        transaction(db) {
            ImageFiles.deleteWhere { ImageFiles.id eq id }
        }
    }

    /**
     * 删除指定图库中的单张图片
     */
    fun deleteImageByMd5(groupId: String, galleryName: String, md5: String): Boolean {
        return transaction(db) {
            // 先查询文件信息
            val imageFile = ImageFiles.selectAll()
                .where { 
                    (ImageFiles.qq eq groupId) and 
                    (ImageFiles.about eq galleryName) and 
                    (ImageFiles.md5 eq md5)
                }.firstOrNull()
            
            if (imageFile != null) {
                // 删除数据库记录
                val deletedCount = ImageFiles.deleteWhere { 
                    (ImageFiles.qq eq groupId) and 
                    (ImageFiles.about eq galleryName) and 
                    (ImageFiles.md5 eq md5)
                }
                
                // 删除本地文件
                if (deletedCount > 0) {
                    try {
                        val fileType = imageFile[ImageFiles.type]
                        val filePath = "LaiZhi/$groupId/$galleryName/$md5.$fileType"
                        val file = PluginMain.resolveDataFile(filePath)
                        if (file.exists()) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        PluginMain.logger.error("删除本地文件失败: ${e.message}")
                    }
                }
                
                deletedCount > 0
            } else {
                false
            }
        }
    }

    /**
     * 获取指定图库的所有图片
     */
    fun selectImagesByGallery(groupId: Long, galleryName: String): List<ImageFile> {
        return transaction(db) {
            ImageFiles.selectAll()
                .where { (ImageFiles.qq eq groupId.toString()) and (ImageFiles.about eq galleryName) }
                .map {
                    ImageFile(
                        it[ImageFiles.id] ?: 0,
                        it[ImageFiles.md5] ?: "",
                        it[ImageFiles.qq] ?: "",
                        it[ImageFiles.count] ?: 0,
                        it[ImageFiles.about] ?: "",
                        it[ImageFiles.type] ?: "",
                        it[ImageFiles.url] ?: ""
                    )
                }
        }
    }

    /**
     * 计算MD5
     */
    fun getMD5(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("MD5")
        md.update(bytes)
        val digest = md.digest()
        return BigInteger(1, digest).toString(16).padStart(32, '0')
    }


    fun getGroup(groupId: String): GroupDetail {
        return transaction {
            var entity = GroupDetails.selectAll()
                .where {
                    GroupDetails.id eq groupId
                }.firstOrNull()

            if (entity == null) {
                GroupDetails.insert {
                    it[id] = groupId
                    it[key] = "0"
                }
                entity = GroupDetails.selectAll()
                    .where {
                        GroupDetails.id eq groupId
                    }.first()
            }
            var res = GroupDetail()
            res.id = groupId;
            res.key = entity[key];
            res
        }
    }

    fun setKey(groupId: String): Boolean {
        return transaction {
            val entity = getGroup(groupId)
            val k1 = entity.key;
            GroupDetails.update({ GroupDetails.id eq groupId }) {
                it[key] = if (entity.key.toString() == "0") "1" else "0"
            }
            k1=="0"
        }
    }
    fun updateMapAfterInsert(qqid: String, name: String) {
        synchronized(DataMP) {
            if (DataMP.containsKey(qqid)) {
                DataMP[qqid]!!.add(name)
            } else {
                DataMP[qqid] = hashSetOf(name)
            }
        }
    }
    fun queryDataToMap(): HashMap<String, HashSet<String>> {
        val dataMap = HashMap<String, HashSet<String>>()
        transaction {
            ImageFiles.selectAll().forEach { row ->
                val qqid = row[ImageFiles.qq]
                val name = row[ImageFiles.about]
                if (dataMap.containsKey(qqid)) {
                    dataMap[qqid]!!.add(name)
                } else {
                    dataMap[qqid] = hashSetOf(name)
                }
            }
        }
        return dataMap
    }
    fun isPathSafe(fileName: String): Boolean {
        val normalizedName = fileName
            .replace("./", "")
            .replace(".\\", "")
            .replace("/./", "/")
            .replace("\\.\\", "\\")

        return normalizedName.matches(Regex("^[a-zA-Z0-9\u4e00-\u9fa5]+$"))
            && !normalizedName.contains("..")
            && !normalizedName.contains(":")
            && !normalizedName.startsWith("/")
            && !normalizedName.startsWith("\\")
    }
}