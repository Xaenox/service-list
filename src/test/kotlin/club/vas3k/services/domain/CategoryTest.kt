package club.vas3k.services.domain

import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class CategoryTest {

    @Test
    fun `toResponse should convert Category to CategoryResponse`() {
        val now = Clock.System.now()
        val categoryId = UUID.randomUUID()
        val parentId = UUID.randomUUID()

        val category = Category(
            id = categoryId,
            name = "Web Development",
            slug = "web-dev",
            description = "All about web development",
            icon = "globe",
            parentId = parentId,
            sortOrder = 1,
            createdAt = now
        )

        val response = category.toResponse()

        assertEquals(categoryId.toString(), response.id)
        assertEquals("Web Development", response.name)
        assertEquals("web-dev", response.slug)
        assertEquals("All about web development", response.description)
        assertEquals("globe", response.icon)
        assertEquals(parentId.toString(), response.parentId)
        assertEquals(1, response.sortOrder)
        assertEquals(0, response.children.size)
    }

    @Test
    fun `toResponse should handle null values`() {
        val now = Clock.System.now()
        val categoryId = UUID.randomUUID()

        val category = Category(
            id = categoryId,
            name = "Design",
            slug = "design",
            description = null,
            icon = null,
            parentId = null,
            sortOrder = 0,
            createdAt = now
        )

        val response = category.toResponse()

        assertEquals(categoryId.toString(), response.id)
        assertEquals("Design", response.name)
        assertEquals("design", response.slug)
        assertEquals(null, response.description)
        assertEquals(null, response.icon)
        assertEquals(null, response.parentId)
        assertEquals(0, response.sortOrder)
    }

    @Test
    fun `toResponse should include children`() {
        val now = Clock.System.now()
        val parentId = UUID.randomUUID()
        val childId = UUID.randomUUID()

        val parent = Category(
            id = parentId,
            name = "Development",
            slug = "development",
            description = "Software Development",
            icon = "code",
            parentId = null,
            sortOrder = 0,
            createdAt = now
        )

        val child = Category(
            id = childId,
            name = "Web Development",
            slug = "web-dev",
            description = "Web apps",
            icon = "globe",
            parentId = parentId,
            sortOrder = 0,
            createdAt = now
        )

        val childResponse = child.toResponse()
        val parentResponse = parent.toResponse(children = listOf(childResponse))

        assertEquals(parentId.toString(), parentResponse.id)
        assertEquals(1, parentResponse.children.size)
        assertEquals(childId.toString(), parentResponse.children[0].id)
        assertEquals("Web Development", parentResponse.children[0].name)
    }
}
