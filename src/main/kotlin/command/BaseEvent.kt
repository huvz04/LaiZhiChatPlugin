package org.huvz.mirai.plugin.command

import com.sun.tools.example.debug.expr.ExpressionParserConstants.IF
import config.LzConfig
import config.LzConfig.AddcommandList
import config.LzConfig.Blacklist
import config.LzConfig.GetcommandList
import config.LzConfig.Graphicslist
import config.LzConfig.adminQQid
import config.LzConfig.clearlist
import config.LzConfig.enablelist
import config.LzConfig.deleteImagelist
import config.LzConfig.previewImagelist
import config.LzConfig.helplist
import config.LzConfig.drawMultipleList
import config.LzConfig.maxDrawCount
import entity.LZException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.time.withTimeoutOrNull
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.ListeningStatus
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import okhttp3.Request
import org.huvz.mirai.plugin.PluginMain
import org.huvz.mirai.plugin.PluginMain.DataMP
import org.huvz.mirai.plugin.PluginMain.logger
import org.huvz.mirai.plugin.Service.ImageService
import org.huvz.mirai.plugin.Service.ImageService.getGroup
import org.huvz.mirai.plugin.Service.ImageService.isPathSafe
import org.huvz.mirai.plugin.Service.ImageService.setKey
import org.huvz.mirai.plugin.command.BaseEvent.blacklist
import org.huvz.mirai.plugin.entity.GroupDetail
import org.huvz.mirai.plugin.entity.ImageFile
import org.huvz.mirai.plugin.util.HttpClient
import org.huvz.mirai.plugin.util.ImageUtils
import org.huvz.mirai.plugin.util.SendTask
import org.huvz.mirai.plugin.util.SendTask.Companion.sendMessage
import org.huvz.mirai.plugin.util.skia.GalleryDetailComposer
import util.skia.ImageDrawerComposer
import org.huvz.mirai.plugin.util.skia.impl.GalleryDetailDrawer
import java.io.File
import java.io.IOException
import java.time.Duration
import kotlin.coroutines.CoroutineContext

object BaseEvent : SimpleListenerHost() {
    // 任何异常将在 handleException 处理
    override fun handleException(context: CoroutineContext, exception: Throwable) {
        PluginMain.logger.error("未知错误")
    }
    val  blacklist: List<String> = Blacklist

    @EventHandler
    suspend fun GroupMessageEvent.onMessage(): ListeningStatus {
        val msg = message.content;
        val keyword = enablelist.firstOrNull() { msg.equals(it) }
        when (msg) {
            keyword -> setKeyWord()
            else -> {
                //add
                val Aprefix = AddcommandList.firstOrNull() { msg.startsWith(it) }
                //get
                val Gprefix = GetcommandList.firstOrNull() { msg.startsWith(it) }
                //list
                val Lapfix = Graphicslist.firstOrNull() { msg.equals(it) }
                //clear
                val Cperfix = clearlist.firstOrNull() { msg.startsWith(it) }
                //delete single image
                val Dprefix = deleteImagelist.firstOrNull() { msg.startsWith(it) }
                //preview gallery
                val Pprefix = previewImagelist.firstOrNull() { msg.startsWith(it) }
                //help
                val Hprefix = helplist.firstOrNull() { msg.equals(it) }
                //draw multiple
                val Mprefix = drawMultipleList.firstOrNull() { msg.startsWith(it) }
                if (Aprefix != null) {
                    val urlname = msg.drop(Aprefix.length).trim()
                    val ck = isPathSafe(urlname)
                    if (ck && !blacklist.contains(urlname)) {
                        Lzsave(urlname, sender)
                        return ListeningStatus.LISTENING
                    } else if (blacklist.contains(urlname)) {
                        // 忽略黑名单图库，不做任何处理
                        return ListeningStatus.LISTENING
                    } else {
                        sendMessage(group, "非法名字！")
                    }
                } else if (Lapfix != null) {
                    getlist(sender);
                    return ListeningStatus.LISTENING
                } else if (Hprefix != null) {
                    showHelp();
                    return ListeningStatus.LISTENING
                } else if (Mprefix != null) {
                    val params = msg.drop(Mprefix.length).trim().split(" ")
                    if (params.size >= 2) {
                        val countStr = params[0]
                        val galleryName = params[1]
                        try {
                            val count = countStr.toInt()
                            if (count > 0 && count <= maxDrawCount) {
                                if (!blacklist.contains(galleryName)) {
                                    drawMultipleImages(galleryName, count)
                                }
                                // 如果是黑名单图库，静默忽略
                            } else {
                                sendMessage(group, "抽取次数必须在1到${maxDrawCount}之间")
                            }
                        } catch (e: NumberFormatException) {
                            sendMessage(group, "请输入有效的数字")
                        }
                    } else {
                        sendMessage(group, "格式错误！请使用：抽 [次数] [图库名]")
                    }
                    return ListeningStatus.LISTENING
                } else if (Dprefix != null) {
                    val params = msg.drop(Dprefix.length).trim().split(" ")
                    if (params.size >= 2) {
                        val galleryName = params[0]
                        val imageNumber = params[1]
                        deleteImageFromGallery(galleryName, imageNumber)
                    } else {
                        sendMessage(group, "格式错误！请使用：删除图片 [图库名] [序号]")
                    }
                    return ListeningStatus.LISTENING
                } else if (Pprefix != null) {
                    val galleryName = msg.drop(Pprefix.length).trim()
                    if (galleryName.isNotEmpty()) {
                        previewGallery(galleryName)
                    } else {
                        sendMessage(group, "请指定图库名称！")
                    }
                    return ListeningStatus.LISTENING
                }

                else if(Cperfix!=null){
                    var name =  msg.replace(Cperfix,"")
                    if(adminQQid != sender.id.toString()){
                        sendMessage(group,At(sender)+"你没有权限执行此操作")
                    }
                    else{
                        if(!isPathSafe(name)){
                            sendMessage(group, "非法名字！")
                        }
                        else{
                            clear(name);
                        }
                    }

                }
                else{
                    if (getGroup(this.group.id.toString()).key == "1") {
                        if(ImageService.clearImage(msg,group.id.toString())>0)
                        if(!blacklist.contains(msg)){
                            val list = DataMP[this.group.id.toString()]
                            var firstMatched: String? = null
                            if (list != null) {
                                for (item in list) {
                                    if (msg.equals(item)) {
                                        firstMatched = item
                                        break
                                    }
                                }
                            }
                            if (firstMatched != null && msg.indexOf(firstMatched)!=-1) {
                                getImg(firstMatched, -1,0)
                            }
                            return ListeningStatus.LISTENING
                        }

                    } else if (Gprefix != null) {
                        // -1 随机
                        var getnum = -1
                        val strlist = msg.split(" ")
                        var urlname = if (strlist.isNotEmpty()) {
                            if (strlist[0].length > 2) {
                                strlist[0].drop(2)
                            } else {
                                strlist[1].trim()
                            }
                        } else {
                            msg.drop(Gprefix.length).trim()
                        }
                        if(blacklist.contains(urlname))
                        {
                            // 忽略黑名单图库，不做任何处理
                            return ListeningStatus.LISTENING
                        }
                        if (strlist.size == 2 || (strlist.size == 3 && strlist[0].length > 2)) {
                            try {
                                getnum = (strlist.getOrNull(1)?.toInt() ?: strlist.getOrNull(2)?.toInt())!!
                            } catch (e: Exception) {
                                PluginMain.logger.error("转换错误，请确认参数是否为int/Long类型")
                            }
                        }
                        getImg(urlname, getnum,1)
                        return ListeningStatus.LISTENING
                    }
                }

            }

        }

        return ListeningStatus.LISTENING
    }

    private suspend fun GroupMessageEvent.setKeyWord() {
        var res = setKey(group.id.toString());
        if (res) sendMessage(group, "当前模式：关键字匹配")
        else sendMessage(group, "当前模式：来只匹配")
    }

    /**
     * 获取图片
     */
    private suspend fun GroupMessageEvent.getImg(arg: String?, arg1: Int,mode:Int) {
        if (arg != null) {
            val res = ImageUtils.GetImage(group, arg, arg1)
            if (res != null) {
                try {
                    this.subject.let {
                        val img = res.uploadAsImage(it)
                        res.closed
                        sendMessage(group, img);
                    }
                }catch (e:Exception){
                    res.closed
                }

            } else {
//                res?.closed
//                if(mode==1)
//                    sendMessage(group, "目录下找不到图片噢")
            }
        }
    }

    /**
     * 清理图库
     */
    private suspend fun GroupMessageEvent.clear(filename: String) {
//        if (filename in LzConfig.ProtectImageList)
//            this.group.sendMessage(At(sender) + "这是受保护的图库，你无法删除噢")
//        else {
            var file = File(PluginMain.dataFolderPath.toString() + "/LaiZhi/${this.group.id}/$filename")
            try {
                val cnt  = ImageService.clearImage(filename,group.id.toString())
                file.deleteRecursively()
                this.group.sendMessage("已经清理了${filename}中的${cnt}条数据")
            } catch (e: Exception) {
                this.group.sendMessage("未知错误")
                e.printStackTrace()
            }
//        }

    }
    /**
     * 添加图片
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun GroupMessageEvent.Lzsave(arg: String?, sender1: Member) {
        SendTask.sendMessage(group, At(sender1) + "请在30s内发送一张图片")
        val duration = Duration.ofMillis(30000)
        var isImageSaved = false
        try{
        withTimeoutOrNull(duration) {
            suspendCancellableCoroutine { continuation ->
                val listener = globalEventChannel().subscribeAlways<GroupMessageEvent> {
                    if (this.sender.id != sender1.id) return@subscribeAlways
                    if(!isImageSaved){
                        var chain = this.message
                        val image = chain.findIsInstance<Image>()
                        if (image != null) {
                            try {
                                val url2 = image.queryUrl()
                                val request = Request.Builder()
                                    .url(url2)
                                    .build()
                                val response = HttpClient.okHttpClient.newCall(request).execute()
                                val contentType = response.header("Content-Type")
                                val fileType = when (contentType) {
                                    "image/jpeg" -> "jpg"
                                    "image/png" -> "png"
                                    "image/gif" -> "gif"
                                    else -> "jpg"
                                }

                                val imageByte = response.body!!.bytes()
                                arg?.let { it1 -> ImageService.saveImage(this.subject.id, it1, imageByte, fileType) }
                                sendMessage(group, chain + PlainText("保存成功噢"))
                                isImageSaved = true
                                continuation.resume(Unit){}
                            } catch (e: LZException) {
                                sendMessage(group, "该图库已存在相同图片哦")
                                isImageSaved = true
                                continuation.resume(Unit){}
                            }
                        }
                    }

                }
                if(isImageSaved) {
//                    PluginMain.logger.info("协程已关闭")
                    listener.complete()
                }
            }


        }}catch (e: TimeoutCancellationException){
            if (!isImageSaved) {
                sendMessage(group, At(sender1) + "已超时，请重新发送图片")
            }
        }

    }

    /**
     * 获取图库
     */
    suspend fun GroupMessageEvent.getlist(sender: Member) {
        val request = Request.Builder()
            .url(sender.group.avatarUrl)
            .build()
        val reponse = request?.let { HttpClient.okHttpClient.newCall(it).execute() }
        val imageByte = reponse?.body!!.bytes()
        val list: List<ImageFile> = ImageService.selectImageDetail(sender.group.id)
        val groupDetail = GroupDetail(
            sender.id.toString(),
            imageByte,
            sender.group.name,
            "",
            list.size,
            sender.group.members.size
        );
        val newMap: HashMap<String, ArrayList<ImageFile>> = hashMapOf()
        for (i in list) {
            // 跳过黑名单图库
            if (blacklist.contains(i.about)) {
                continue
            }
            
            var list2: ArrayList<ImageFile> = arrayListOf()
            try {
                if (newMap[i.about] != null) {
                    list2 = newMap[i.about]!!
                }
                list2.add(i)
                newMap[i.about] = list2
            } catch (_: Exception) {
                PluginMain.logger.error("读取图片错误:i.url+\\${i.md5}.${i.type}....自动移除脏数据")
                ImageService.deleteImage(i.id);
            }
        }
        val composer = ImageDrawerComposer(
            1430, (newMap.size / 6 + 1) * (185 + 40) + 200,
            "titleText", newMap, 6,
            groupDetail,
            40f,
            100,
            185f
        )
        try {
            val res = composer.draw();
            this.subject.let {
                val img = res.uploadAsImage(it)
                sendMessage(group, img);
            }
        } catch (e: Exception) {
            sendMessage(this.subject, "图片发送失败呜呜\n异常:${e.message}")
        }
    }


    fun isPathSafe(fileName: String): Boolean {
        try {
            val normalizedName = fileName
                .replace("./", "")
                .replace(".\\", "")
                .replace("/./", "/")
                .replace("\\.\\", "\\")
            if(fileName.length<30)
                return normalizedName.matches(Regex("^[a-zA-Z0-9\u4e00-\u9fa5]+$"))
                    && !normalizedName.contains("..")
                    && !normalizedName.contains(":")
                    && !normalizedName.startsWith("/")
                    && !normalizedName.startsWith("\\")
            return false
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * 删除图库中的单张图片（使用序号）
     */
    private suspend fun GroupMessageEvent.deleteImageFromGallery(galleryName: String, imageNumber: String) {
        if (adminQQid != sender.id.toString()) {
            sendMessage(group, At(sender) + "你没有权限执行此操作")
            return
        }
        
        if (!isPathSafe(galleryName)) {
            sendMessage(group, "非法图库名称！")
            return
        }
        
        try {
            val number = imageNumber.toIntOrNull()
            if (number == null || number <= 0) {
                sendMessage(group, "请输入有效的序号（正整数）")
                return
            }
            
            // 获取图库中的所有图片
            val images = ImageService.selectImagesByGallery(group.id, galleryName)
            if (images.isEmpty()) {
                sendMessage(group, "图库 $galleryName 中没有图片")
                return
            }
            
            if (number > images.size) {
                sendMessage(group, "序号超出范围！图库 $galleryName 中只有 ${images.size} 张图片")
                return
            }
            
            // 获取要删除的图片（序号从1开始，数组索引从0开始）
            val imageToDelete = images[number - 1]
            val success = ImageService.deleteImageByMd5(group.id.toString(), galleryName, imageToDelete.md5)
            
            if (success) {
                sendMessage(group, "已成功删除图库 $galleryName 中的第 $number 张图片")
            } else {
                sendMessage(group, "删除失败，未找到指定的图片")
            }
        } catch (e: NumberFormatException) {
            sendMessage(group, "请输入有效的序号（正整数）")
        } catch (e: Exception) {
            sendMessage(group, "删除图片时发生错误: ${e.message}")
            PluginMain.logger.error("删除图片错误", e)
        }
    }

    /**
     * 预览图库中的所有图片
     */
    private suspend fun GroupMessageEvent.previewGallery(galleryName: String) {
        if (!isPathSafe(galleryName)) {
            sendMessage(group, "非法图库名称！")
            return
        }
        
        try {
            val images = ImageService.selectImagesByGallery(group.id, galleryName)
            if (images.isEmpty()) {
                sendMessage(group, "图库 $galleryName 中没有图片")
                return
            }
            // 计算输出高度，与GalleryDetailDrawer保持一致
            val rows = (images.size + 6 - 1) / 6
            val outputHeight = 100 + 40 + rows * (185f + 40f + 40f) + 40
            
            val drawer = GalleryDetailComposer(
                outputWidth = 1430,
                outputHeight = outputHeight.toInt(),
                titleText = "图库详情",
                galleryName = galleryName,
                imageList = images,
                lt = 40f,
                infoHeight = 100,
                targetSize = 185f
            )
//            val drawer = GalleryDetailComposer(galleryName, images)
            val resultFile = drawer.draw()
            
            this.subject.let {
                val img = resultFile.uploadAsImage(it)
                SendTask.sendMessage(group, img)
            }
            
            // 清理临时文件
            resultFile.delete()
        } catch (e: Exception) {
            sendMessage(group, "预览图库时发生错误: ${e.message}")
            PluginMain.logger.error("预览图库错误", e)
        }
    }

    /**
     * 显示帮助信息
     */
    private suspend fun GroupMessageEvent.showHelp() {
        val helpText = buildString {
            appendLine("📚 来只管理帮助")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("🔍 查看图库：")
            appendLine("  ${Graphicslist.joinToString("、")} - 查看所有图库")
            appendLine()
            appendLine("🖼️ 获取图片：")
            appendLine("  ${GetcommandList.joinToString("、")} [图库名] - 随机获取图片")
            appendLine("  ${GetcommandList.joinToString("、")} [图库名] [数量] - 获取指定数量图片")
            appendLine()
            appendLine("🎲 抽取多次：")
            appendLine("  ${drawMultipleList.joinToString("、")} [次数] [图库名] - 一次性抽取多张图片")
            appendLine()
            appendLine("➕ 添加图片：")
            appendLine("  ${AddcommandList.joinToString("、")} [图库名] - 添加图片到指定图库")
            appendLine()
            appendLine("🗑️ 管理图库：")
            appendLine("  ${clearlist.joinToString("、")} [图库名] - 清空整个图库 (仅管理员)")
            appendLine("  ${deleteImagelist.joinToString("、")} [图库名] [序号] - 删除单张图片 (仅管理员)")
            appendLine()
            appendLine("👀 预览图库：")
            appendLine("  ${previewImagelist.joinToString("、")} [图库名] - 预览图库所有图片")
            appendLine()
            appendLine("❓ 其他：")
            appendLine("  ${helplist.joinToString("、")} - 显示此帮助信息")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("💡 提示：图片序号可通过预览图库命令获取，序号从1开始")
            appendLine("💡 抽取次数限制：1-${maxDrawCount}次")
        }
        
        sendMessage(group, helpText)
    }

    /**
     * 抽取多张图片并使用转发消息发送
     */
    private suspend fun GroupMessageEvent.drawMultipleImages(galleryName: String, count: Int) {
        if (!isPathSafe(galleryName)) {
            sendMessage(group, "非法图库名称！")
            return
        }
        
        try {
            val images = mutableListOf<Image>()
            
            // 获取多张图片
            for (i in 1..count) {
                val res = ImageUtils.GetImage(group, galleryName, -1) // -1表示随机
                if (res != null) {
                    try {
                        val img = res.uploadAsImage(subject)
                        images.add(img)
                        res.close()
                    } catch (e: Exception) {
                        res.close()
                        PluginMain.logger.error("上传图片失败", e)
                    }
                }
            }
            
            if (images.isNotEmpty()) {
                // 构建转发消息
                val forwardMessage = buildForwardMessage(subject) {
                    // 添加标题消息
                    add(
                        senderId = bot.id,
                        senderName = bot.nick,
                        time = (System.currentTimeMillis() / 1000).toInt()
                    ) {
                        + PlainText("🎲 从图库「$galleryName」抽取了${images.size}张图片")
                    }
                    
                    // 添加所有图片消息
                    images.forEachIndexed { index, img ->
                        add(
                            senderId = bot.id,
                            senderName = bot.nick,
                            time = (System.currentTimeMillis() / 1000).toInt()
                        ) {
                            + PlainText("第${index + 1}张")
                            + img
                        }
                    }
                }
                
                // 发送转发消息
                group.sendMessage(forwardMessage)
            } else {
                sendMessage(group, "图库 $galleryName 中没有图片或图片获取失败")
            }
            
        } catch (e: Exception) {
            sendMessage(group, "抽取图片时发生错误: ${e.message}")
            PluginMain.logger.error("抽取多张图片错误", e)
        }
    }

}