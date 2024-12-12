package org.huvz.mirai.plugin.entity

public data class ImageFile(
    var id: Long,
    var md5: String,
    var qq: String,
    var count :Long,
    var about:String,
    var type:String,
    var url:String){
    constructor() : this(1, "2", "你群", 0L, "", "", "")
}
