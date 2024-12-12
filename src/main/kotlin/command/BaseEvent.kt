package org.huvz.mirai.plugin.command

import config.LzConfig
import config.LzConfig.AddcommandList
import config.LzConfig.GetcommandList
import config.LzConfig.Graphicslist
import config.LzConfig.enablelist
import entity.LZException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.time.withTimeoutOrNull
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.getGroupOrNull
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.contact.Group
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
import org.huvz.mirai.plugin.Service.ImageService
import org.huvz.mirai.plugin.Service.ImageService.setKey
import org.huvz.mirai.plugin.entity.GroupDetail
import org.huvz.mirai.plugin.entity.ImageFile
import org.huvz.mirai.plugin.util.HttpClient
import org.huvz.mirai.plugin.util.ImageUtils
import org.huvz.mirai.plugin.util.SendTask
import org.huvz.mirai.plugin.util.SendTask.Companion.sendMessage
import util.skia.ImageDrawerComposer
import java.io.File
import java.io.IOException
import java.time.Duration
import kotlin.coroutines.CoroutineContext

object BaseEvent : SimpleListenerHost() {
    // 任何异常将在 handleException 处理
    override fun handleException(context: CoroutineContext, exception: Throwable) {
        PluginMain.logger.error("未知错误")
    }

    var isKey: Boolean = false;

    @EventHandler
    suspend fun GroupMessageEvent.onMessage(): ListeningStatus {
        val msg = message.content;
        val urlname: String?
        val keyword = enablelist.firstOrNull() { msg.equals(it) }
        when (msg) {

            keyword -> setKeyWord()
            else -> {
                //add
                val Aprefix = AddcommandList.firstOrNull() { msg.startsWith(it) }
                //get
                val Gprefix = GetcommandList.firstOrNull() { msg.startsWith(it) }
                //list
                val gapfix =Graphicslist.firstOrNull() { msg.equals(it) }
                if (Aprefix!=null) {
                    val urlname = msg.drop(Aprefix.length).trim()
                    val ck = isPathSafe(urlname)
                    if (ck) {
                        Lzsave(urlname, sender)
                    } else {
                        sendMessage(group, "非法名字！")
                    }
                }
                else if (Gprefix!=null) {
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
                    if (strlist.size == 2 || (strlist.size == 3 && strlist[0].length > 2)) {
                        try {
                            getnum = (strlist.getOrNull(1)?.toInt() ?: strlist.getOrNull(2)?.toInt())!!
                        } catch (e: Exception) {
                            PluginMain.logger.error("转换错误，请确认参数是否为int/Long类型")
                        }
                    }
                    getImg(urlname, getnum)
                }
                else if(gapfix!=null){
                    getlist(sender);
                }
            }
        }
        return ListeningStatus.LISTENING
    }
    private suspend fun GroupMessageEvent.setKeyWord(){
        var res = setKey(group.id.toString());
        if(res) sendMessage(group,"当前模式：关键字匹配")
        else sendMessage(group,"当前模式：来只匹配")
    }
    /**
     * 获取图片
     */
    private suspend fun GroupMessageEvent.getImg(arg: String?, arg1: Int) {
        if (arg != null) {
            val res = ImageUtils.GetImage(group, arg, arg1)
            if (res != null) {
                this.subject.let {
                    val img = res.uploadAsImage(it)
                    res.closed
                    sendMessage(group, img);
                }
            } else {
                sendMessage(group, "目录下找不到图片噢")
            }
        }
    }

    /**
     * 清理图库
     */
//    private suspend fun GroupMessageEvent.clear(filename: String) {
//        if (filename in LzConfig.ProtectImageList)
//            this.group.sendMessage(At(sender) + "这是受保护的图库，你无法删除噢")
//        else {
//            var file = File(PluginMain.dataFolderPath.toString() + "/LaiZhi/${this.group.id}/$filename")
//            try {
//                file.deleteRecursively()
//            } catch (e: Exception) {
//                this.group.sendMessage("未知错误")
//                e.printStackTrace()
//            }
//        }
//
//    }
    /**
     * 添加图片
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun GroupMessageEvent.Lzsave(arg: String?, sender1: Member) {
        SendTask.sendMessage(group, At(sender1) + "请在30s内发送一张图片")
        val duration = Duration.ofMillis(3000)
        withTimeoutOrNull(duration) {
            suspendCancellableCoroutine { continuation ->
                val listener = globalEventChannel().subscribeAlways<GroupMessageEvent> {
                    if (this.sender.id != sender1.id) return@subscribeAlways
                    var chain = this.message;
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
                            sendMessage(group, chain + PlainText("保存成功噢"));
                        } catch (e: LZException) {
                            sendMessage(group, "该图库已存在相同图片哦")
                        }
                        continuation.resume(Unit) {}
                    }
                }
                continuation.invokeOnCancellation {
                    listener.complete()
                }
            }
        } ?: run {
            sendMessage(group, At(sender1) + "已超时，请重新发送图片")
        }
    }

    /**
     * 获取图库
     */
    suspend fun GroupMessageEvent.getlist(sender: Member){
        val request = Request.Builder()
                .url(sender.group.avatarUrl)
                .build()
        val reponse = request?.let { HttpClient.okHttpClient.newCall(it).execute() }
        val imageByte = reponse?.body!!.bytes()
        val list : List<ImageFile> = ImageService.selectImageDetail(sender.group.id)
        val groupDetail = GroupDetail(
            sender.id.toString(),
            imageByte,
            sender.group.name,
            "",
            list.size,
            sender.group.members.size
        );
        val newMap:HashMap<String,ArrayList<ImageFile>> = hashMapOf()
        for(i in list){
            var list2 : ArrayList<ImageFile> = arrayListOf()
            try{
                if(newMap[i.about] !=null) {
                    list2 = newMap[i.about]!!
                }
                list2.add(i)
                newMap[i.about] = list2
            }catch (_:Exception){
                PluginMain.logger.error("读取图片错误:i.url+\\${i.md5}.${i.type}....自动移除脏数据")
                ImageService.deleteImage(i.id);
            }
        }
        val composer = ImageDrawerComposer(
            1430, (newMap.size/6+1)*(185+40)+200,
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
        }catch (e:Exception)
        {
            sendMessage(this.subject,"图片发送失败呜呜\n异常:${e.message}")
        }
    }


    fun isPathSafe(fileName: String): Boolean {
        try {
            val normalizedName = fileName
                .replace("./", "")
                .replace(".\\", "")
                .replace("/./", "/")
                .replace("\\.\\", "\\")

            return normalizedName.matches(Regex("^[a-zA-Z0-9\u4e00-\u9fa5]+\\.[a-zA-Z0-9]+$"))
                && !normalizedName.contains("..")
                && !normalizedName.contains(":")
                && !normalizedName.startsWith("/")
                && !normalizedName.startsWith("\\")
        } catch (e: IOException) {
            return false
        }
    }

}