package config


import net.mamoe.mirai.console.data.*


@PublishedApi
internal object LzConfig : AutoSavePluginConfig("LaiZhiConfig"){
    /**
     * 获取指令列表
     * 默认为"[来点]","[来只]"
     */
    @ValueDescription("触发图库指令")
    val GetcommandList:List<String> by value(listOf("来点","来只"))
    @ValueDescription("添加图库指令")
    val AddcommandList:List<String> by value(listOf("add","添加"))
    @ValueDescription("管理员QQ")
    var adminQQid:String by value("123456")
    @ValueDescription("消息发送延迟")
    var messageIntervalTime :Long by value(120L);
    @ValueDescription("黑名单图库")
    var Blacklist:List<String> by value(listOf("图片","帮助"))
    @ValueDescription("获取图库指令列表")
    var Graphicslist:List<String> by value(listOf("#获取图库","#图库","本群图库"))
    @ValueDescription("关键字匹配")
    var enablelist:List<String> by value(listOf("开关关键字"))
    @ValueDescription("清空图库指令")
    var clearlist:List<String> by value(listOf("#清理"))
    @ValueDescription("删除图库内单张图片指令")
    var deleteImagelist:List<String> by value(listOf("#删除"))
    @ValueDescription("预览图库内所有图片指令")
    var previewImagelist:List<String> by value(listOf("#预览"))
    @ValueDescription("帮助指令")
    var helplist:List<String> by value(listOf("来只帮助","#帮助"))
    @ValueDescription("抽取多次指令")
    var drawMultipleList:List<String> by value(listOf("抽"))
    @ValueDescription("抽取多次最大数量限制")
    var maxDrawCount:Int by value(10)
}