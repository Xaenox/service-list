package club.vas3k.services.repository

import club.vas3k.services.database.*
import club.vas3k.services.domain.*
import club.vas3k.services.util.TestDatabaseFactory
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

class ServiceRepositoryTest {

    private lateinit var serviceRepository: ServiceRepository
    private lateinit var userRepository: UserRepository
    private lateinit var categoryRepository: CategoryRepository

    @BeforeEach
    fun setup() {
        TestDatabaseFactory.init()
        userRepository = UserRepository()
        categoryRepository = CategoryRepository()
        serviceRepository = ServiceRepository(userRepository, categoryRepository)
    }

    @AfterEach
    fun tearDown() {
        transaction {
            ServiceCategories.deleteAll()
            Services.deleteAll()
            Categories.deleteAll()
            Users.deleteAll()
        }
    }

    @Test
    fun `create should create new service with details`() = runBlocking {
        val user = userRepository.create("owner", "owner@example.com", "Owner", null, null, null)
        val category = categoryRepository.create("Tech", "tech", null, null, null, 0)

        val serviceWithDetails = serviceRepository.create(
            title = "Web Development Service",
            description = "Professional web development",
            type = ServiceType.ONLINE,
            contacts = Contacts(email = "service@example.com", phone = "+1234567890"),
            location = Location(country = "USA", city = "New York"),
            bonus = Bonus(type = BonusType.DISCOUNT_PERCENT, value = "10", description = "10% off"),
            tags = listOf("web", "development"),
            ownerId = user.id,
            categoryIds = listOf(category.id)
        )

        assertNotNull(serviceWithDetails)
        assertEquals("Web Development Service", serviceWithDetails.service.title)
        assertEquals("Professional web development", serviceWithDetails.service.description)
        assertEquals(ServiceType.ONLINE, serviceWithDetails.service.type)
        assertEquals(ServiceStatus.ACTIVE, serviceWithDetails.service.status)
        assertEquals("service@example.com", serviceWithDetails.service.contacts.email)
        assertEquals("+1234567890", serviceWithDetails.service.contacts.phone)
        assertEquals("USA", serviceWithDetails.service.location?.country)
        assertEquals("New York", serviceWithDetails.service.location?.city)
        assertEquals(BonusType.DISCOUNT_PERCENT, serviceWithDetails.service.bonus?.type)
        assertEquals(listOf("web", "development"), serviceWithDetails.service.tags)
        assertEquals(user.id, serviceWithDetails.owner.id)
        assertEquals(1, serviceWithDetails.categories.size)
        assertEquals(category.id, serviceWithDetails.categories[0].id)
    }

    @Test
    fun `findById should return service when exists`() = runBlocking {
        val user = userRepository.create("user", "user@example.com", "User", null, null, null)
        val created = serviceRepository.create(
            title = "Test Service",
            description = "Test Description",
            type = ServiceType.OFFLINE,
            contacts = Contacts(email = "test@example.com"),
            location = null,
            bonus = null,
            tags = emptyList(),
            ownerId = user.id,
            categoryIds = emptyList()
        )

        val found = serviceRepository.findById(created!!.service.id)

        assertNotNull(found)
        assertEquals(created.service.id, found.service.id)
        assertEquals("Test Service", found.service.title)
    }

    @Test
    fun `findById should return null when service does not exist`() = runBlocking {
        val found = serviceRepository.findById(java.util.UUID.randomUUID())
        assertNull(found)
    }

    @Test
    fun `findAll should return paginated services`() = runBlocking {
        val user = userRepository.create("user", "user@example.com", "User", null, null, null)

        serviceRepository.create("Service 1", "Desc 1", ServiceType.ONLINE, Contacts(email = "1@example.com"), null, null, emptyList(), user.id, emptyList())
        serviceRepository.create("Service 2", "Desc 2", ServiceType.OFFLINE, Contacts(email = "2@example.com"), null, null, emptyList(), user.id, emptyList())
        serviceRepository.create("Service 3", "Desc 3", ServiceType.HYBRID, Contacts(email = "3@example.com"), null, null, emptyList(), user.id, emptyList())

        val (services, total) = serviceRepository.findAll(
            filter = ServiceFilter(status = ServiceStatus.ACTIVE),
            page = 1,
            pageSize = 2
        )

        assertEquals(2, services.size)
        assertEquals(3, total)
    }

    @Test
    fun `findAll should filter by query`() = runBlocking {
        val user = userRepository.create("user", "user@example.com", "User", null, null, null)

        serviceRepository.create("Web Development", "Build websites", ServiceType.ONLINE, Contacts(email = "web@example.com"), null, null, emptyList(), user.id, emptyList())
        serviceRepository.create("Mobile App", "Build apps", ServiceType.ONLINE, Contacts(email = "mobile@example.com"), null, null, emptyList(), user.id, emptyList())

        val (services, total) = serviceRepository.findAll(
            filter = ServiceFilter(query = "web", status = ServiceStatus.ACTIVE),
            page = 1,
            pageSize = 10
        )

        assertEquals(1, services.size)
        assertEquals(1, total)
        assertEquals("Web Development", services[0].service.title)
    }

    @Test
    fun `findAll should filter by type`() = runBlocking {
        val user = userRepository.create("user", "user@example.com", "User", null, null, null)

        serviceRepository.create("Online Service", "Online", ServiceType.ONLINE, Contacts(email = "online@example.com"), null, null, emptyList(), user.id, emptyList())
        serviceRepository.create("Offline Service", "Offline", ServiceType.OFFLINE, Contacts(email = "offline@example.com"), null, null, emptyList(), user.id, emptyList())

        val (services, total) = serviceRepository.findAll(
            filter = ServiceFilter(type = ServiceType.ONLINE, status = ServiceStatus.ACTIVE),
            page = 1,
            pageSize = 10
        )

        assertEquals(1, services.size)
        assertEquals(1, total)
        assertEquals("Online Service", services[0].service.title)
    }

    @Test
    fun `findAll should filter by city`() = runBlocking {
        val user = userRepository.create("user", "user@example.com", "User", null, null, null)

        serviceRepository.create("NY Service", "In NY", ServiceType.ONLINE, Contacts(email = "ny@example.com"), Location(city = "New York"), null, emptyList(), user.id, emptyList())
        serviceRepository.create("LA Service", "In LA", ServiceType.ONLINE, Contacts(email = "la@example.com"), Location(city = "Los Angeles"), null, emptyList(), user.id, emptyList())

        val (services, total) = serviceRepository.findAll(
            filter = ServiceFilter(city = "New York", status = ServiceStatus.ACTIVE),
            page = 1,
            pageSize = 10
        )

        assertEquals(1, services.size)
        assertEquals("NY Service", services[0].service.title)
    }

    @Test
    fun `findAll should filter by hasBonus`() = runBlocking {
        val user = userRepository.create("user", "user@example.com", "User", null, null, null)

        serviceRepository.create("With Bonus", "Has bonus", ServiceType.ONLINE, Contacts(email = "bonus@example.com"), null, Bonus(BonusType.GIFT, null, "Free gift"), emptyList(), user.id, emptyList())
        serviceRepository.create("Without Bonus", "No bonus", ServiceType.ONLINE, Contacts(email = "nobonus@example.com"), null, null, emptyList(), user.id, emptyList())

        val (servicesWithBonus, totalWithBonus) = serviceRepository.findAll(
            filter = ServiceFilter(hasBonus = true, status = ServiceStatus.ACTIVE),
            page = 1,
            pageSize = 10
        )

        assertEquals(1, servicesWithBonus.size)
        assertEquals("With Bonus", servicesWithBonus[0].service.title)

        val (servicesWithoutBonus, totalWithoutBonus) = serviceRepository.findAll(
            filter = ServiceFilter(hasBonus = false, status = ServiceStatus.ACTIVE),
            page = 1,
            pageSize = 10
        )

        assertEquals(1, servicesWithoutBonus.size)
        assertEquals("Without Bonus", servicesWithoutBonus[0].service.title)
    }

    @Test
    fun `findAll should filter by category slug`() = runBlocking {
        val user = userRepository.create("user", "user@example.com", "User", null, null, null)
        val cat1 = categoryRepository.create("Tech", "tech", null, null, null, 0)
        val cat2 = categoryRepository.create("Design", "design", null, null, null, 0)

        serviceRepository.create("Tech Service", "Tech", ServiceType.ONLINE, Contacts(email = "tech@example.com"), null, null, emptyList(), user.id, listOf(cat1.id))
        serviceRepository.create("Design Service", "Design", ServiceType.ONLINE, Contacts(email = "design@example.com"), null, null, emptyList(), user.id, listOf(cat2.id))

        val (services, total) = serviceRepository.findAll(
            filter = ServiceFilter(categorySlug = "tech", status = ServiceStatus.ACTIVE),
            page = 1,
            pageSize = 10
        )

        assertEquals(1, services.size)
        assertEquals("Tech Service", services[0].service.title)
    }

    @Test
    fun `findByOwnerId should return services by owner`() = runBlocking {
        val user1 = userRepository.create("user1", "user1@example.com", "User 1", null, null, null)
        val user2 = userRepository.create("user2", "user2@example.com", "User 2", null, null, null)

        serviceRepository.create("User1 Service 1", "Desc", ServiceType.ONLINE, Contacts(email = "u1s1@example.com"), null, null, emptyList(), user1.id, emptyList())
        serviceRepository.create("User1 Service 2", "Desc", ServiceType.ONLINE, Contacts(email = "u1s2@example.com"), null, null, emptyList(), user1.id, emptyList())
        serviceRepository.create("User2 Service 1", "Desc", ServiceType.ONLINE, Contacts(email = "u2s1@example.com"), null, null, emptyList(), user2.id, emptyList())

        val (services, total) = serviceRepository.findByOwnerId(user1.id, page = 1, pageSize = 10)

        assertEquals(2, services.size)
        assertEquals(2, total)
        assertTrue(services.all { it.owner.id == user1.id })
    }

    @Test
    fun `update should update service fields`() = runBlocking {
        val user = userRepository.create("user", "user@example.com", "User", null, null, null)
        val service = serviceRepository.create(
            "Original Title",
            "Original Description",
            ServiceType.ONLINE,
            Contacts(email = "original@example.com"),
            null,
            null,
            emptyList(),
            user.id,
            emptyList()
        )

        val updated = serviceRepository.update(
            id = service!!.service.id,
            title = "Updated Title",
            description = "Updated Description",
            type = ServiceType.OFFLINE,
            status = null,
            contacts = Contacts(email = "updated@example.com"),
            location = Location(city = "Moscow"),
            bonus = Bonus(BonusType.GIFT, null, "Gift"),
            tags = listOf("updated"),
            categoryIds = null
        )

        assertTrue(updated)

        val found = serviceRepository.findById(service.service.id)
        assertNotNull(found)
        assertEquals("Updated Title", found.service.title)
        assertEquals("Updated Description", found.service.description)
        assertEquals(ServiceType.OFFLINE, found.service.type)
    }

    @Test
    fun `delete should soft delete service`() = runBlocking {
        val user = userRepository.create("user", "user@example.com", "User", null, null, null)
        val service = serviceRepository.create(
            "To Delete",
            "Description",
            ServiceType.ONLINE,
            Contacts(email = "delete@example.com"),
            null,
            null,
            emptyList(),
            user.id,
            emptyList()
        )

        val deleted = serviceRepository.delete(service!!.service.id)
        assertTrue(deleted)

        val found = serviceRepository.findById(service.service.id)
        assertNotNull(found)
        assertEquals(ServiceStatus.DELETED, found.service.status)
    }

    @Test
    fun `hardDelete should permanently remove service`() = runBlocking {
        val user = userRepository.create("user", "user@example.com", "User", null, null, null)
        val service = serviceRepository.create(
            "To Hard Delete",
            "Description",
            ServiceType.ONLINE,
            Contacts(email = "harddelete@example.com"),
            null,
            null,
            emptyList(),
            user.id,
            emptyList()
        )

        val deleted = serviceRepository.hardDelete(service!!.service.id)
        assertTrue(deleted)

        val found = serviceRepository.findById(service.service.id)
        assertNull(found)
    }

    @Test
    fun `isOwner should return true when user owns service`() = runBlocking {
        val user = userRepository.create("user", "user@example.com", "User", null, null, null)
        val service = serviceRepository.create(
            "Service",
            "Description",
            ServiceType.ONLINE,
            Contacts(email = "service@example.com"),
            null,
            null,
            emptyList(),
            user.id,
            emptyList()
        )

        val isOwner = serviceRepository.isOwner(service!!.service.id, user.id)
        assertTrue(isOwner)
    }

    @Test
    fun `isOwner should return false when user does not own service`() = runBlocking {
        val user1 = userRepository.create("user1", "user1@example.com", "User 1", null, null, null)
        val user2 = userRepository.create("user2", "user2@example.com", "User 2", null, null, null)
        val service = serviceRepository.create(
            "Service",
            "Description",
            ServiceType.ONLINE,
            Contacts(email = "service@example.com"),
            null,
            null,
            emptyList(),
            user1.id,
            emptyList()
        )

        val isOwner = serviceRepository.isOwner(service!!.service.id, user2.id)
        assertFalse(isOwner)
    }
}
