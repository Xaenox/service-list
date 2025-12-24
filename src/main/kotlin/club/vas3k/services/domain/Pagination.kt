package club.vas3k.services.domain

import kotlinx.serialization.Serializable

@Serializable
data class PagedResponse<T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalItems: Long,
    val totalPages: Int
)

data class ServiceFilter(
    val query: String? = null,
    val categorySlug: String? = null,
    val type: ServiceType? = null,
    val city: String? = null,
    val country: String? = null,
    val hasBonus: Boolean? = null,
    val ownerId: String? = null,
    val status: ServiceStatus = ServiceStatus.ACTIVE
)

fun <T> createPagedResponse(
    items: List<T>,
    page: Int,
    pageSize: Int,
    totalItems: Long
): PagedResponse<T> {
    val totalPages = if (totalItems == 0L) 1 else ((totalItems + pageSize - 1) / pageSize).toInt()
    return PagedResponse(
        items = items,
        page = page,
        pageSize = pageSize,
        totalItems = totalItems,
        totalPages = totalPages
    )
}
