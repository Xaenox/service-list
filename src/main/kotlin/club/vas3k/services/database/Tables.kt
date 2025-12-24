package club.vas3k.services.database

import club.vas3k.services.domain.ServiceStatus
import club.vas3k.services.domain.ServiceType
import club.vas3k.services.domain.UserRole
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Users : UUIDTable("users") {
    val slug = varchar("slug", 100).uniqueIndex()
    val email = varchar("email", 255).uniqueIndex()
    val fullName = varchar("full_name", 255)
    val avatarUrl = varchar("avatar_url", 500).nullable()
    val country = varchar("country", 100).nullable()
    val city = varchar("city", 100).nullable()
    val role = enumerationByName<UserRole>("role", 20).default(UserRole.USER)
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

object Categories : UUIDTable("categories") {
    val name = varchar("name", 100)
    val slug = varchar("slug", 100).uniqueIndex()
    val description = text("description").nullable()
    val icon = varchar("icon", 100).nullable()
    val parentId = reference("parent_id", Categories).nullable()
    val sortOrder = integer("sort_order").default(0)
    val createdAt = timestamp("created_at")
}

object Services : UUIDTable("services") {
    val title = varchar("title", 255)
    val description = text("description")
    val type = enumerationByName<ServiceType>("type", 20)
    val status = enumerationByName<ServiceStatus>("status", 20).default(ServiceStatus.ACTIVE)

    // Contacts as JSON
    val contactsJson = text("contacts_json")

    // Location as separate fields for better querying
    val locationCountry = varchar("location_country", 100).nullable()
    val locationCity = varchar("location_city", 100).nullable()
    val locationAddress = varchar("location_address", 500).nullable()
    val locationLatitude = double("location_latitude").nullable()
    val locationLongitude = double("location_longitude").nullable()

    // Bonus as JSON
    val bonusJson = text("bonus_json").nullable()

    // Tags as JSON array
    val tagsJson = text("tags_json").default("[]")

    val ownerId = reference("owner_id", Users, onDelete = ReferenceOption.CASCADE)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    init {
        index(false, locationCountry)
        index(false, locationCity)
        index(false, type)
        index(false, status)
    }
}

object ServiceCategories : Table("service_categories") {
    val serviceId = reference("service_id", Services, onDelete = ReferenceOption.CASCADE)
    val categoryId = reference("category_id", Categories, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(serviceId, categoryId)
}
