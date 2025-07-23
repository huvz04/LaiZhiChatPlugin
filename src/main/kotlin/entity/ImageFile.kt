package org.huvz.mirai.plugin.entity

public data class ImageFile(
    var id: Long,
    var md5: String,
    var qq: String,
    var count :Long,
    var about:String,
    var type:String,
    var url:String,
    var data: ByteArray? = null){
    constructor() : this(1, "2", "你群", 0L, "", "", "", null)
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageFile

        if (id != other.id) return false
        if (md5 != other.md5) return false
        if (qq != other.qq) return false
        if (count != other.count) return false
        if (about != other.about) return false
        if (type != other.type) return false
        if (url != other.url) return false
        if (data != null) {
            if (other.data == null) return false
            if (!data.contentEquals(other.data)) return false
        } else if (other.data != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + md5.hashCode()
        result = 31 * result + qq.hashCode()
        result = 31 * result + count.hashCode()
        result = 31 * result + about.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + (data?.contentHashCode() ?: 0)
        return result
    }
}
