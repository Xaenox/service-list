package club.vas3k.services.repository

import club.vas3k.services.database.Users
import club.vas3k.services.domain.UserRole
import club.vas3k.services.util.TestDatabaseFactory
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

class UserRepositoryTest {

    private lateinit var repository: UserRepository

    @BeforeEach
    fun setup() {
        TestDatabaseFactory.init()
        repository = UserRepository()
    }

    @AfterEach
    fun tearDown() {
        transaction {
            Users.deleteAll()
        }
    }

    @Test
    fun `create should create new user`() = runBlocking {
        val user = repository.create(
            slug = "john-doe",
            email = "john@example.com",
            fullName = "John Doe",
            avatarUrl = "https://example.com/avatar.jpg",
            country = "USA",
            city = "New York"
        )

        assertNotNull(user.id)
        assertEquals("john-doe", user.slug)
        assertEquals("john@example.com", user.email)
        assertEquals("John Doe", user.fullName)
        assertEquals("https://example.com/avatar.jpg", user.avatarUrl)
        assertEquals("USA", user.country)
        assertEquals("New York", user.city)
        assertEquals(UserRole.USER, user.role)
        assertTrue(user.isActive)
    }

    @Test
    fun `findById should return user when exists`() = runBlocking {
        val created = repository.create(
            slug = "test-user",
            email = "test@example.com",
            fullName = "Test User",
            avatarUrl = null,
            country = null,
            city = null
        )

        val found = repository.findById(created.id)

        assertNotNull(found)
        assertEquals(created.id, found.id)
        assertEquals("test-user", found.slug)
        assertEquals("test@example.com", found.email)
    }

    @Test
    fun `findById should return null when user does not exist`() = runBlocking {
        val found = repository.findById(java.util.UUID.randomUUID())
        assertNull(found)
    }

    @Test
    fun `findBySlug should return user when exists`() = runBlocking {
        repository.create(
            slug = "jane-doe",
            email = "jane@example.com",
            fullName = "Jane Doe",
            avatarUrl = null,
            country = null,
            city = null
        )

        val found = repository.findBySlug("jane-doe")

        assertNotNull(found)
        assertEquals("jane-doe", found.slug)
        assertEquals("jane@example.com", found.email)
    }

    @Test
    fun `findBySlug should return null when user does not exist`() = runBlocking {
        val found = repository.findBySlug("non-existent")
        assertNull(found)
    }

    @Test
    fun `findByEmail should return user when exists`() = runBlocking {
        repository.create(
            slug = "email-test",
            email = "unique@example.com",
            fullName = "Email Test",
            avatarUrl = null,
            country = null,
            city = null
        )

        val found = repository.findByEmail("unique@example.com")

        assertNotNull(found)
        assertEquals("unique@example.com", found.email)
        assertEquals("email-test", found.slug)
    }

    @Test
    fun `findByEmail should return null when user does not exist`() = runBlocking {
        val found = repository.findByEmail("nonexistent@example.com")
        assertNull(found)
    }

    @Test
    fun `update should update user fields`() = runBlocking {
        val user = repository.create(
            slug = "update-test",
            email = "update@example.com",
            fullName = "Original Name",
            avatarUrl = null,
            country = null,
            city = null
        )

        val updated = repository.update(
            id = user.id,
            fullName = "Updated Name",
            avatarUrl = "https://example.com/new-avatar.jpg",
            country = "Russia",
            city = "Moscow"
        )

        assertTrue(updated)

        val found = repository.findById(user.id)
        assertNotNull(found)
        assertEquals("Updated Name", found.fullName)
        assertEquals("https://example.com/new-avatar.jpg", found.avatarUrl)
        assertEquals("Russia", found.country)
        assertEquals("Moscow", found.city)
    }

    @Test
    fun `update should only update provided fields`() = runBlocking {
        val user = repository.create(
            slug = "partial-update",
            email = "partial@example.com",
            fullName = "Original Name",
            avatarUrl = "https://example.com/avatar.jpg",
            country = "USA",
            city = "New York"
        )

        val updated = repository.update(
            id = user.id,
            fullName = "New Name"
        )

        assertTrue(updated)

        val found = repository.findById(user.id)
        assertNotNull(found)
        assertEquals("New Name", found.fullName)
        assertEquals("https://example.com/avatar.jpg", found.avatarUrl)
        assertEquals("USA", found.country)
        assertEquals("New York", found.city)
    }

    @Test
    fun `updateRole should change user role`() = runBlocking {
        val user = repository.create(
            slug = "role-test",
            email = "role@example.com",
            fullName = "Role Test",
            avatarUrl = null,
            country = null,
            city = null
        )

        assertEquals(UserRole.USER, user.role)

        val updated = repository.updateRole(user.id, UserRole.MODERATOR)
        assertTrue(updated)

        val found = repository.findById(user.id)
        assertNotNull(found)
        assertEquals(UserRole.MODERATOR, found.role)
    }

    @Test
    fun `upsertFromOAuth should create new user when not exists`() = runBlocking {
        val user = repository.upsertFromOAuth(
            slug = "oauth-new",
            email = "oauth@example.com",
            fullName = "OAuth User",
            avatarUrl = "https://example.com/oauth-avatar.jpg",
            country = "UK",
            city = "London"
        )

        assertNotNull(user.id)
        assertEquals("oauth-new", user.slug)
        assertEquals("oauth@example.com", user.email)
        assertEquals("OAuth User", user.fullName)
    }

    @Test
    fun `upsertFromOAuth should update existing user`() = runBlocking {
        val original = repository.create(
            slug = "oauth-existing",
            email = "old@example.com",
            fullName = "Old Name",
            avatarUrl = null,
            country = null,
            city = null
        )

        val updated = repository.upsertFromOAuth(
            slug = "oauth-existing",
            email = "new@example.com",
            fullName = "New Name",
            avatarUrl = "https://example.com/new.jpg",
            country = "France",
            city = "Paris"
        )

        assertEquals(original.id, updated.id)
        assertEquals("oauth-existing", updated.slug)
        assertEquals("new@example.com", updated.email)
        assertEquals("New Name", updated.fullName)
        assertEquals("https://example.com/new.jpg", updated.avatarUrl)
        assertEquals("France", updated.country)
        assertEquals("Paris", updated.city)
    }

    @Test
    fun `upsertFromOAuth should preserve user role on update`() = runBlocking {
        val original = repository.create(
            slug = "oauth-role",
            email = "role@example.com",
            fullName = "Role User",
            avatarUrl = null,
            country = null,
            city = null
        )

        repository.updateRole(original.id, UserRole.ADMIN)

        repository.upsertFromOAuth(
            slug = "oauth-role",
            email = "updated@example.com",
            fullName = "Updated User",
            avatarUrl = null,
            country = null,
            city = null
        )

        val found = repository.findById(original.id)
        assertNotNull(found)
        assertEquals(UserRole.ADMIN, found.role)
    }
}
