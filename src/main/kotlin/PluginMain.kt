package org.huvz.mirai.plugin



import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.globalEventChannel
import config.LzConfig
import net.mamoe.mirai.utils.MiraiLogger
import org.huvz.mirai.plugin.Service.ImageService
import org.huvz.mirai.plugin.command.BaseEvent


object PluginMain : KotlinPlugin(
    JvmPluginDescription(
        id = "org.huvz.laizhi",
        name = "LaiZhiXX",
        version = "0.4.0"
    ) {
        author("Huvz")
        info(
            """
            来只&来点 功能 将群友话语做成可以出发的图
        """.trimIndent()
        )
//        dependsOn("xyz.cssxsh.mirai.plugin.mirai-hibernate-plugin", false)
    }
) {


    override fun onEnable() {

        LzConfig.reload()
        ImageService.initDatabase()

        globalEventChannel().registerListenerHost(BaseEvent)
        logger.info("Plugin loaded" )
        //        CommandManager.registerCommand(List2Image)
//        println(ImageService.selectImageDetail(114514))
    }

    override fun onDisable() {

    }

}


