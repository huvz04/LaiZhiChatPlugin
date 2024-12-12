package entity.data

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object ImageFiles : Table("image_file") {
    val id: Column<Long> = long("id")
    val md5 = varchar("md5", 32)
    val qq = varchar("qq", 20)
    val count = long("count")
    val about = varchar("about", 100)
    val type = varchar("type", 10)
    val url = varchar("url", 200)
    override val primaryKey = PrimaryKey(id)
}