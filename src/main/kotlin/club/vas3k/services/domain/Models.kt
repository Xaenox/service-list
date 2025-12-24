package club.vas3k.services.domain

import kotlinx.serialization.Serializable
import java.util.UUID

enum class ServiceType {
    ONLINE,
    OFFLINE,
    HYBRID
}

enum class ServiceStatus {
    ACTIVE,
    INACTIVE,
    DELETED
}

enum class UserRole {
    USER,
    MODERATOR,
    ADMIN
}

enum class BonusType {
    DISCOUNT_PERCENT,
    DISCOUNT_FIXED,
    FREE_TRIAL,
    GIFT,
    CUSTOM
}

@Serializable
data class Contacts(
    val email: String? = null,
    val phone: String? = null,
    val telegram: String? = null,
    val whatsapp: String? = null,
    val website: String? = null
)

@Serializable
data class Location(
    val country: String? = null,
    val city: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class Bonus(
    val type: BonusType,
    val value: String? = null,
    val description: String
)
