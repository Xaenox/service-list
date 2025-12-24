package club.vas3k.services.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.UUID

data class User(
    val id: UUID,
    val slug: String,
    val email: String,
    val fullName: String,
    val avatarUrl: String?,
    val country: String?,
    val city: String?,
    val role: UserRole,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class UserResponse(
    val id: String,
    val slug: String,
    val fullName: String,
    val avatarUrl: String?,
    val country: String?,
    val city: String?
)

@Serializable
data class UserProfileResponse(
    val id: String,
    val slug: String,
    val email: String,
    val fullName: String,
    val avatarUrl: String?,
    val country: String?,
    val city: String?,
    val role: String,
    val createdAt: String
)

fun User.toResponse() = UserResponse(
    id = id.toString(),
    slug = slug,
    fullName = fullName,
    avatarUrl = avatarUrl,
    country = country,
    city = city
)

fun User.toProfileResponse() = UserProfileResponse(
    id = id.toString(),
    slug = slug,
    email = email,
    fullName = fullName,
    avatarUrl = avatarUrl,
    country = country,
    city = city,
    role = role.name,
    createdAt = createdAt.toString()
)
