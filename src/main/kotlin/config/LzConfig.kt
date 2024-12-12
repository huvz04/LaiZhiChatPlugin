package config


import net.mamoe.mirai.console.data.*


@PublishedApi
internal object LzConfig : AutoSavePluginConfig("LaiZhiConfig"){
    /**
     * 获取指令列表
     * 默认为"[来点]","[来只]"
     */
    @ValueDescription("触发-添加图库-指令")
    val GetcommandList:List<String> by value(listOf("来点","来只"))
    @ValueDescription("触发-添加图库-指令")
    val AddcommandList:List<String> by value(listOf("add","添加"))

    @ValueDescription("管理员QQ")
    var adminQQid:Long by value()


    @ValueDescription("消息发送延迟")
    var messageIntervalTime :Long by value(120L);

//    @ValueDescription("黑名单")
//    var Blacklist:List<Long> by value()

    @ValueDescription("获取图库指令列表")
    var Graphicslist:List<String> by value(listOf("#获取图库","#图库","本群图库"))
    @ValueDescription("开关关键字")
    var enablelist:List<String> by value(listOf("开关关键字"))

}