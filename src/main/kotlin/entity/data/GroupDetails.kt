package entity.data

import org.jetbrains.exposed.sql.Table

object GroupDetails : Table("group_detail") {
    val id = varchar("id", 20)
    val avatar = binary("avatar")
    val name = varchar("name", 50)
    val key = varchar("key",2)
    val total = integer("total")
    val galleryNumber = integer("galleryNumber")
}