package club.vas3k.services.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.UUID

data class Service(
    val id: UUID,
    val title: String,
    val description: String,
    val type: ServiceType,
    val status: ServiceStatus,
    val contacts: Contacts,
    val location: Location?,
    val bonus: Bonus?,
    val tags: List<String>,
    val ownerId: UUID,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class ServiceWithDetails(
    val service: Service,
    val owner: User,
    val categories: List<Category>
)

@Serializable
data class ServiceResponse(
    val id: String,
    val title: String,
    val description: String,
    val type: String,
    val status: String,
    val contacts: Contacts,
    val location: Location?,
    val bonus: Bonus?,
    val tags: List<String>,
    val owner: UserResponse,
    val categories: List<CategoryResponse>,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ServiceListItemResponse(
    val id: String,
    val title: String,
    val description: String,
    val type: String,
    val location: Location?,
    val bonus: Bonus?,
    val owner: UserResponse,
    val categories: List<CategoryResponse>,
    val createdAt: String
)

@Serializable
data class CreateServiceRequest(
    val title: String,
    val description: String,
    val type: ServiceType,
    val contacts: Contacts,
    val location: Location? = null,
    val bonus: Bonus? = null,
    val tags: List<String> = emptyList(),
    val categoryIds: List<String> = emptyList()
)

@Serializable
data class UpdateServiceRequest(
    val title: String? = null,
    val description: String? = null,
    val type: ServiceType? = null,
    val status: ServiceStatus? = null,
    val contacts: Contacts? = null,
    val location: Location? = null,
    val bonus: Bonus? = null,
    val tags: List<String>? = null,
    val categoryIds: List<String>? = null
)

fun ServiceWithDetails.toResponse() = ServiceResponse(
    id = service.id.toString(),
    title = service.title,
    description = service.description,
    type = service.type.name,
    status = service.status.name,
    contacts = service.contacts,
    location = service.location,
    bonus = service.bonus,
    tags = service.tags,
    owner = owner.toResponse(),
    categories = categories.map { it.toResponse() },
    createdAt = service.createdAt.toString(),
    updatedAt = service.updatedAt.toString()
)

fun ServiceWithDetails.toListItemResponse() = ServiceListItemResponse(
    id = service.id.toString(),
    title = service.title,
    description = service.description.take(200) + if (service.description.length > 200) "..." else "",
    type = service.type.name,
    location = service.location,
    bonus = service.bonus,
    owner = owner.toResponse(),
    categories = categories.map { it.toResponse() },
    createdAt = service.createdAt.toString()
)
