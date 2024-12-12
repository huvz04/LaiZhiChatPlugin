package org.huvz.mirai.plugin.entity


public data class GroupDetail(
    var id:String,
    val avatar:ByteArray,
    val name:String,
    var key:String,
    var total:Int,
    val galleryNumber:Int,
) {
    // 添加无参的辅助构造函数
    constructor() : this("114514", ByteArray(0), "群聊","0", 0, 0)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as GroupDetail

        if (id != other.id) return false
        if (!avatar.contentEquals(other.avatar)) return false
        if (name != other.name) return false
        if (total != other.total) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + avatar.contentHashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + total
        return result
    }

}