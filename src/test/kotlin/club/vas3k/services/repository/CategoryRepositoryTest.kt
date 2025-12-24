package club.vas3k.services.repository

import club.vas3k.services.database.Categories
import club.vas3k.services.util.TestDatabaseFactory
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

class CategoryRepositoryTest {

    private lateinit var repository: CategoryRepository

    @BeforeEach
    fun setup() {
        TestDatabaseFactory.init()
        repository = CategoryRepository()
    }

    @AfterEach
    fun tearDown() {
        transaction {
            Categories.deleteAll()
        }
    }

    @Test
    fun `create should create new category`() = runBlocking {
        val category = repository.create(
            name = "Web Development",
            slug = "web-dev",
            description = "All about web development",
            icon = "globe",
            parentId = null,
            sortOrder = 1
        )

        assertNotNull(category.id)
        assertEquals("Web Development", category.name)
        assertEquals("web-dev", category.slug)
        assertEquals("All about web development", category.description)
        assertEquals("globe", category.icon)
        assertNull(category.parentId)
        assertEquals(1, category.sortOrder)
    }

    @Test
    fun `findById should return category when exists`() = runBlocking {
        val created = repository.create(
            name = "Design",
            slug = "design",
            description = null,
            icon = null,
            parentId = null,
            sortOrder = 0
        )

        val found = repository.findById(created.id)

        assertNotNull(found)
        assertEquals(created.id, found.id)
        assertEquals("Design", found.name)
        assertEquals("design", found.slug)
    }

    @Test
    fun `findById should return null when category does not exist`() = runBlocking {
        val found = repository.findById(java.util.UUID.randomUUID())
        assertNull(found)
    }

    @Test
    fun `findBySlug should return category when exists`() = runBlocking {
        repository.create(
            name = "Marketing",
            slug = "marketing",
            description = "Marketing services",
            icon = "megaphone",
            parentId = null,
            sortOrder = 0
        )

        val found = repository.findBySlug("marketing")

        assertNotNull(found)
        assertEquals("Marketing", found.name)
        assertEquals("marketing", found.slug)
    }

    @Test
    fun `findBySlug should return null when category does not exist`() = runBlocking {
        val found = repository.findBySlug("non-existent")
        assertNull(found)
    }

    @Test
    fun `findAll should return all categories sorted`() = runBlocking {
        repository.create("Zulu", "zulu", null, null, null, 3)
        repository.create("Alpha", "alpha", null, null, null, 1)
        repository.create("Bravo", "bravo", null, null, null, 2)

        val all = repository.findAll()

        assertEquals(3, all.size)
        assertEquals("Alpha", all[0].name)
        assertEquals("Bravo", all[1].name)
        assertEquals("Zulu", all[2].name)
    }

    @Test
    fun `findByIds should return matching categories`() = runBlocking {
        val cat1 = repository.create("Cat1", "cat1", null, null, null, 0)
        val cat2 = repository.create("Cat2", "cat2", null, null, null, 0)
        val cat3 = repository.create("Cat3", "cat3", null, null, null, 0)

        val found = repository.findByIds(listOf(cat1.id, cat3.id))

        assertEquals(2, found.size)
        assertTrue(found.any { it.id == cat1.id })
        assertTrue(found.any { it.id == cat3.id })
        assertFalse(found.any { it.id == cat2.id })
    }

    @Test
    fun `findByIds should return empty list for empty input`() = runBlocking {
        val found = repository.findByIds(emptyList())
        assertTrue(found.isEmpty())
    }

    @Test
    fun `findRootCategories should return only categories without parent`() = runBlocking {
        val parent = repository.create("Parent", "parent", null, null, null, 1)
        repository.create("Root1", "root1", null, null, null, 2)
        repository.create("Child", "child", null, null, parent.id, 0)
        repository.create("Root2", "root2", null, null, null, 3)

        val roots = repository.findRootCategories()

        assertEquals(2, roots.size)
        assertTrue(roots.all { it.parentId == null })
        assertEquals("Parent", roots[0].name)
        assertEquals("Root1", roots[1].name)
    }

    @Test
    fun `findChildren should return categories with specified parent`() = runBlocking {
        val parent1 = repository.create("Parent1", "parent1", null, null, null, 0)
        val parent2 = repository.create("Parent2", "parent2", null, null, null, 0)

        repository.create("Child1-1", "child1-1", null, null, parent1.id, 2)
        repository.create("Child1-2", "child1-2", null, null, parent1.id, 1)
        repository.create("Child2-1", "child2-1", null, null, parent2.id, 0)

        val children = repository.findChildren(parent1.id)

        assertEquals(2, children.size)
        assertTrue(children.all { it.parentId == parent1.id })
        assertEquals("Child1-2", children[0].name) // Sorted by sortOrder
        assertEquals("Child1-1", children[1].name)
    }

    @Test
    fun `update should update category fields`() = runBlocking {
        val category = repository.create(
            name = "Original",
            slug = "original",
            description = "Original description",
            icon = "icon1",
            parentId = null,
            sortOrder = 0
        )

        val updated = repository.update(
            id = category.id,
            name = "Updated",
            description = "Updated description",
            icon = "icon2",
            sortOrder = 5
        )

        assertTrue(updated)

        val found = repository.findById(category.id)
        assertNotNull(found)
        assertEquals("Updated", found.name)
        assertEquals("Updated description", found.description)
        assertEquals("icon2", found.icon)
        assertEquals(5, found.sortOrder)
        assertEquals("original", found.slug) // Slug should not change
    }

    @Test
    fun `update should only update provided fields`() = runBlocking {
        val category = repository.create(
            name = "Original",
            slug = "original",
            description = "Original description",
            icon = "icon1",
            parentId = null,
            sortOrder = 1
        )

        val updated = repository.update(
            id = category.id,
            name = "Updated Name",
            description = null,
            icon = null,
            sortOrder = null
        )

        assertTrue(updated)

        val found = repository.findById(category.id)
        assertNotNull(found)
        assertEquals("Updated Name", found.name)
        assertEquals("Original description", found.description)
        assertEquals("icon1", found.icon)
        assertEquals(1, found.sortOrder)
    }

    @Test
    fun `delete should remove category`() = runBlocking {
        val category = repository.create("ToDelete", "to-delete", null, null, null, 0)

        val deleted = repository.delete(category.id)
        assertTrue(deleted)

        val found = repository.findById(category.id)
        assertNull(found)
    }

    @Test
    fun `delete should return false when category does not exist`() = runBlocking {
        val deleted = repository.delete(java.util.UUID.randomUUID())
        assertFalse(deleted)
    }

    @Test
    fun `existsBySlug should return true when slug exists`() = runBlocking {
        repository.create("Exists", "exists-slug", null, null, null, 0)

        val exists = repository.existsBySlug("exists-slug")
        assertTrue(exists)
    }

    @Test
    fun `existsBySlug should return false when slug does not exist`() = runBlocking {
        val exists = repository.existsBySlug("non-existent-slug")
        assertFalse(exists)
    }
}
