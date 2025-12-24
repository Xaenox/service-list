package club.vas3k.services.domain

import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UserTest {

    @Test
    fun `toResponse should convert User to UserResponse`() {
        val now = Clock.System.now()
        val userId = UUID.randomUUID()
        val user = User(
            id = userId,
            slug = "test-user",
            email = "test@example.com",
            fullName = "Test User",
            avatarUrl = "https://example.com/avatar.jpg",
            country = "Russia",
            city = "Moscow",
            role = UserRole.USER,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )

        val response = user.toResponse()

        assertEquals(userId.toString(), response.id)
        assertEquals("test-user", response.slug)
        assertEquals("Test User", response.fullName)
        assertEquals("https://example.com/avatar.jpg", response.avatarUrl)
        assertEquals("Russia", response.country)
        assertEquals("Moscow", response.city)
    }

    @Test
    fun `toProfileResponse should convert User to UserProfileResponse`() {
        val now = Clock.System.now()
        val userId = UUID.randomUUID()
        val user = User(
            id = userId,
            slug = "test-user",
            email = "test@example.com",
            fullName = "Test User",
            avatarUrl = "https://example.com/avatar.jpg",
            country = "Russia",
            city = "Moscow",
            role = UserRole.MODERATOR,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )

        val response = user.toProfileResponse()

        assertEquals(userId.toString(), response.id)
        assertEquals("test-user", response.slug)
        assertEquals("test@example.com", response.email)
        assertEquals("Test User", response.fullName)
        assertEquals("https://example.com/avatar.jpg", response.avatarUrl)
        assertEquals("Russia", response.country)
        assertEquals("Moscow", response.city)
        assertEquals("MODERATOR", response.role)
        assertNotNull(response.createdAt)
    }

    @Test
    fun `toResponse should handle null values`() {
        val now = Clock.System.now()
        val userId = UUID.randomUUID()
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

        val response = user.toResponse()

        assertEquals(userId.toString(), response.id)
        assertEquals("test-user", response.slug)
        assertEquals("Test User", response.fullName)
        assertEquals(null, response.avatarUrl)
        assertEquals(null, response.country)
        assertEquals(null, response.city)
    }
}
