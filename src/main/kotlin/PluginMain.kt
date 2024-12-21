package org.huvz.mirai.plugin



import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.globalEventChannel
import config.LzConfig
import net.mamoe.mirai.utils.MiraiLogger
import org.huvz.mirai.plugin.Service.ImageService
import org.huvz.mirai.plugin.Service.ImageService.queryDataToMap
import org.huvz.mirai.plugin.command.BaseEvent


object PluginMain : KotlinPlugin(
    JvmPluginDescription(
        id = "org.huvz.laizhi",
        name = "LaiZhiXX",
        version = "0.5.1"
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

    var DataMP: HashMap<String, HashSet<String>> = hashMapOf()
    override fun onEnable() {

        LzConfig.reload()
        ImageService.initDatabase()
//        logger.info("数据库加载成功，执行脏数据清理中....")
//        var n  =ImageService.deleteUnsafeFiles()
//        logger.info("本次清理掉$n 条脏数据")
        DataMP = queryDataToMap();
        globalEventChannel().registerListenerHost(BaseEvent)
        logger.info("Plugin loaded" )
        //        CommandManager.registerCommand(List2Image)
//        println(ImageService.selectImageDetail(114514))
    }

    override fun onDisable() {

    }

}


