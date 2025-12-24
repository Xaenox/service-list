package club.vas3k.services.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.UUID

data class Category(
    val id: UUID,
    val name: String,
    val slug: String,
    val description: String?,
    val icon: String?,
    val parentId: UUID?,
    val sortOrder: Int,
    val createdAt: Instant
)

@Serializable
data class CategoryResponse(
    val id: String,
    val name: String,
    val slug: String,
    val description: String?,
    val icon: String?,
    val parentId: String?,
    val sortOrder: Int,
    val children: List<CategoryResponse> = emptyList()
)

@Serializable
data class CreateCategoryRequest(
    val name: String,
    val slug: String,
    val description: String? = null,
    val icon: String? = null,
    val parentId: String? = null,
    val sortOrder: Int = 0
)

@Serializable
data class UpdateCategoryRequest(
    val name: String? = null,
    val description: String? = null,
    val icon: String? = null,
    val sortOrder: Int? = null
)

fun Category.toResponse(children: List<CategoryResponse> = emptyList()) = CategoryResponse(
    id = id.toString(),
    name = name,
    slug = slug,
    description = description,
    icon = icon,
    parentId = parentId?.toString(),
    sortOrder = sortOrder,
    children = children
)
