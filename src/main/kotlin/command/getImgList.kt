package command


import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import org.huvz.mirai.plugin.PluginMain
import org.huvz.mirai.plugin.entity.ImageData
import org.huvz.mirai.plugin.util.graphicsUtil
import java.io.File

object getImgList :SimpleCommand(PluginMain,"getGallery",description = "获取全部的图库列表") {

    @Handler
    suspend fun handlerlist(sender: CommandSender){
        val files = File(PluginMain.dataFolder.absolutePath+"\\LaiZhi\\${sender.subject?.id}")
        val images = files.listFiles()
        val simgs =  mutableListOf<ImageData>()
        images?.forEach {
            run {
                var o1 = ImageData(it.name, null, 0);
                var img1 = File(files.absolutePath + "\\${it.name}").listFiles{file -> file.extension == "jpg" || file.extension == "png" || file.extension == "gif"|| file.extension == "jpeg"}
                if (img1 != null) {
                    if(img1.isNotEmpty()) {
                        o1.Img  = img1.get(0)
                        o1.size = img1.size
                    }
                }
                if(o1.Img!=null && o1.size>0)simgs.add(o1)
            }
        }


            try {
                val res = graphicsUtil.draw(simgs);
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