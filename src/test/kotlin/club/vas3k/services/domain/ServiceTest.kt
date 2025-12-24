package club.vas3k.services.domain

import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServiceTest {

    @Test
    fun `toResponse should convert ServiceWithDetails to ServiceResponse`() {
        val now = Clock.System.now()
        val userId = UUID.randomUUID()
        val serviceId = UUID.randomUUID()
        val categoryId = UUID.randomUUID()

        val user = User(
            id = userId,
            slug = "test-user",
            email = "test@example.com",
            fullName = "Test User",
            avatarUrl = null,
            country = "Russia",
            city = "Moscow",
            role = UserRole.USER,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )

        val category = Category(
            id = categoryId,
            name = "Web Development",
            slug = "web-dev",
            description = "Web dev category",
            icon = "icon",
            parentId = null,
            sortOrder = 0,
            createdAt = now
        )

        val service = Service(
            id = serviceId,
            title = "Test Service",
            description = "Test Description",
            type = ServiceType.ONLINE,
            status = ServiceStatus.ACTIVE,
            contacts = Contacts(email = "contact@example.com"),
            location = Location(country = "Russia", city = "Moscow"),
            bonus = Bonus(type = BonusType.DISCOUNT_PERCENT, value = "10", description = "10% off"),
            tags = listOf("tag1", "tag2"),
            ownerId = userId,
            createdAt = now,
            updatedAt = now
        )

        val serviceWithDetails = ServiceWithDetails(
            service = service,
            owner = user,
            categories = listOf(category)
        )

        val response = serviceWithDetails.toResponse()

        assertEquals(serviceId.toString(), response.id)
        assertEquals("Test Service", response.title)
        assertEquals("Test Description", response.description)
        assertEquals("ONLINE", response.type)
        assertEquals("ACTIVE", response.status)
        assertEquals("contact@example.com", response.contacts.email)
        assertEquals("Russia", response.location?.country)
        assertEquals("Moscow", response.location?.city)
        assertEquals(BonusType.DISCOUNT_PERCENT, response.bonus?.type)
        assertEquals(listOf("tag1", "tag2"), response.tags)
        assertEquals(userId.toString(), response.owner.id)
        assertEquals(1, response.categories.size)
        assertEquals(categoryId.toString(), response.categories[0].id)
    }

    @Test
    fun `toListItemResponse should truncate description to 200 characters`() {
        val now = Clock.System.now()
        val userId = UUID.randomUUID()
        val serviceId = UUID.randomUUID()

        val longDescription = "a".repeat(250)

        val user = User(
            id = userId,
            slug = "test-user",
            email = "test@example.com",
            fullName = "Test User",
            avatarUrl = null,
            country = null,
            city = null,
            role = UserRole.USER,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )

        val service = Service(
            id = serviceId,
            title = "Test Service",
            description = longDescription,
            type = ServiceType.ONLINE,
            status = ServiceStatus.ACTIVE,
            contacts = Contacts(email = "contact@example.com"),
            location = null,
            bonus = null,
            tags = emptyList(),
            ownerId = userId,
            createdAt = now,
            updatedAt = now
        )

        val serviceWithDetails = ServiceWithDetails(
            service = service,
            owner = user,
            categories = emptyList()
        )

        val response = serviceWithDetails.toListItemResponse()

        assertEquals(203, response.description.length) // 200 chars + "..."
        assertTrue(response.description.endsWith("..."))
        assertEquals("a".repeat(200) + "...", response.description)
    }

    @Test
    fun `toListItemResponse should not truncate short description`() {
        val now = Clock.System.now()
        val userId = UUID.randomUUID()
        val serviceId = UUID.randomUUID()

        val shortDescription = "Short description"

        val user = User(
            id = userId,
            slug = "test-user",
            email = "test@example.com",
            fullName = "Test User",
            avatarUrl = null,
            country = null,
            city = null,
            role = UserRole.USER,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )

        val service = Service(
            id = serviceId,
            title = "Test Service",
            description = shortDescription,
            type = ServiceType.OFFLINE,
            status = ServiceStatus.ACTIVE,
            contacts = Contacts(phone = "+1234567890"),
            location = null,
            bonus = null,
            tags = emptyList(),
            ownerId = userId,
            createdAt = now,
            updatedAt = now
        )

        val serviceWithDetails = ServiceWithDetails(
            service = service,
            owner = user,
            categories = emptyList()
        )

        val response = serviceWithDetails.toListItemResponse()

        assertEquals(shortDescription, response.description)
    }
}
