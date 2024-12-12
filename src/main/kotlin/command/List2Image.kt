package command


import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.command.getGroupOrNull
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import okhttp3.Request
import org.huvz.mirai.plugin.PluginMain
import org.huvz.mirai.plugin.Service.ImageService
import org.huvz.mirai.plugin.entity.GroupDetail
import org.huvz.mirai.plugin.entity.ImageFile
import org.huvz.mirai.plugin.util.HttpClient
import util.skia.ImageDrawerComposer

object List2Image :SimpleCommand(PluginMain,"获取图库",description = "获取全部的图库列表") {

    @Handler
    suspend fun handlerlist(sender: CommandSender){
        val request = sender.subject?.avatarUrl?.let {
            Request.Builder()
                .url(it)
                .build()
        }
        val reponse = request?.let { HttpClient.okHttpClient.newCall(it).execute() }
        val imageByte = reponse?.body!!.bytes()
        val list : List<ImageFile> = ImageService.selectImageDetail(sender.subject?.id?:114514L)
        val groupDetail = GroupDetail(
            sender.subject?.id.toString(),
            imageByte,
            sender.getGroupOrNull()?.name?:"群聊",
            "0",
            list.size,
            sender.getGroupOrNull()?.members?.size?:0
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
                sender.subject?.let {
                    res.uploadAsImage(it)
                    sender.subject!!.sendImage(res)
                }
            }catch (e:Exception)
            {
                sender.subject?.sendMessage("图片发送失败呜呜\n异常:${e.message}")
            }





    }




}