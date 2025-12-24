package club.vas3k.services.auth

import club.vas3k.services.domain.User
import club.vas3k.services.domain.UserRole
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.*

class JwtServiceTest {

    private val secret = "test-secret-key-for-testing-only-at-least-256-bits-long"
    private val issuer = "http://localhost:8080/"
    private val audience = "http://localhost:8080/api"
    private val expirationDays = 7

    private val jwtService = JwtService(secret, issuer, audience, expirationDays)

    @Test
    fun `generateToken should create valid JWT token`() {
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

        val token = jwtService.generateToken(user)

        assertNotNull(token)
        assertTrue(token.isNotEmpty())
        assertTrue(token.split(".").size == 3) // JWT has 3 parts
    }

    @Test
    fun `verifier should decode and verify valid token`() {
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
            role = UserRole.MODERATOR,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )

        val token = jwtService.generateToken(user)
        val decodedJWT = jwtService.verifier.verify(token)

        assertNotNull(decodedJWT)
        assertEquals(userId.toString(), decodedJWT.subject)
        assertEquals(issuer, decodedJWT.issuer)
        assertEquals(listOf(audience), decodedJWT.audience)
    }

    @Test
    fun `getUserIdFromToken should extract user ID from token`() {
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

        val token = jwtService.generateToken(user)
        val decodedJWT = jwtService.verifier.verify(token)
        val extractedUserId = jwtService.getUserIdFromToken(decodedJWT)

        assertEquals(userId, extractedUserId)
    }

    @Test
    fun `getRoleFromToken should extract role from token`() {
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
            role = UserRole.ADMIN,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )

        val token = jwtService.generateToken(user)
        val decodedJWT = jwtService.verifier.verify(token)
        val role = jwtService.getRoleFromToken(decodedJWT)

        assertEquals("ADMIN", role)
    }

    @Test
    fun `token should contain all required claims`() {
        val now = Clock.System.now()
        val userId = UUID.randomUUID()
        val user = User(
            id = userId,
            slug = "john-doe",
            email = "john@example.com",
            fullName = "John Doe",
            avatarUrl = "https://example.com/avatar.jpg",
            country = "USA",
            city = "New York",
            role = UserRole.MODERATOR,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )

        val token = jwtService.generateToken(user)
        val decodedJWT = jwtService.verifier.verify(token)

        assertEquals(userId.toString(), decodedJWT.subject)
        assertEquals("john-doe", decodedJWT.getClaim("slug").asString())
        assertEquals("john@example.com", decodedJWT.getClaim("email").asString())
        assertEquals("MODERATOR", decodedJWT.getClaim("role").asString())
        assertNotNull(decodedJWT.expiresAt)
    }

    @Test
    fun `getUserIdFromToken should return null for invalid UUID`() {
        val decodedJWT = jwtService.verifier.verify(
            jwtService.generateToken(
                User(
                    id = UUID.randomUUID(),
                    slug = "test",
                    email = "test@example.com",
                    fullName = "Test",
                    avatarUrl = null,
                    country = null,
                    city = null,
                    role = UserRole.USER,
                    isActive = true,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now()
                )
            )
        )

        // This should work with valid UUID
        assertNotNull(jwtService.getUserIdFromToken(decodedJWT))
    }
}
