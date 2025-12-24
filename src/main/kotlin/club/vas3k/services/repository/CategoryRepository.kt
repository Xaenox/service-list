package club.vas3k.services.repository

import club.vas3k.services.database.Categories
import club.vas3k.services.database.dbQuery
import club.vas3k.services.domain.Category
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.UUID

class CategoryRepository {

    suspend fun findAll(): List<Category> = dbQuery {
        Categories.selectAll()
            .orderBy(Categories.sortOrder to SortOrder.ASC, Categories.name to SortOrder.ASC)
            .map { it.toCategory() }
    }

    suspend fun findById(id: UUID): Category? = dbQuery {
        Categories.selectAll().where { Categories.id eq id }
            .map { it.toCategory() }
            .singleOrNull()
    }

    suspend fun findBySlug(slug: String): Category? = dbQuery {
        Categories.selectAll().where { Categories.slug eq slug }
            .map { it.toCategory() }
            .singleOrNull()
    }

    suspend fun findByIds(ids: List<UUID>): List<Category> = dbQuery {
        if (ids.isEmpty()) return@dbQuery emptyList()
        Categories.selectAll().where { Categories.id inList ids }
            .map { it.toCategory() }
    }

    suspend fun findRootCategories(): List<Category> = dbQuery {
        Categories.selectAll().where { Categories.parentId.isNull() }
            .orderBy(Categories.sortOrder to SortOrder.ASC)
            .map { it.toCategory() }
    }

    suspend fun findChildren(parentId: UUID): List<Category> = dbQuery {
        Categories.selectAll().where { Categories.parentId eq parentId }
            .orderBy(Categories.sortOrder to SortOrder.ASC)
            .map { it.toCategory() }
    }

    suspend fun create(
        name: String,
        slug: String,
        description: String?,
        icon: String?,
        parentId: UUID?,
        sortOrder: Int
    ): Category = dbQuery {
        val now = Clock.System.now()
        val id = UUID.randomUUID()

        Categories.insert {
            it[Categories.id] = id
            it[Categories.name] = name
            it[Categories.slug] = slug
            it[Categories.description] = description
            it[Categories.icon] = icon
            it[Categories.parentId] = parentId
            it[Categories.sortOrder] = sortOrder
            it[Categories.createdAt] = now
        }

        Category(
            id = id,
            name = name,
            slug = slug,
            description = description,
            icon = icon,
            parentId = parentId,
            sortOrder = sortOrder,
            createdAt = now
        )
    }

    suspend fun update(
        id: UUID,
        name: String?,
        description: String?,
        icon: String?,
        sortOrder: Int?
    ): Boolean = dbQuery {
        Categories.update({ Categories.id eq id }) {
            name?.let { n -> it[Categories.name] = n }
            description?.let { d -> it[Categories.description] = d }
            icon?.let { i -> it[Categories.icon] = i }
            sortOrder?.let { s -> it[Categories.sortOrder] = s }
        } > 0
    }

    suspend fun delete(id: UUID): Boolean = dbQuery {
        Categories.deleteWhere { Categories.id eq id } > 0
    }

    suspend fun existsBySlug(slug: String): Boolean = dbQuery {
        Categories.selectAll().where { Categories.slug eq slug }.count() > 0
    }

    private fun ResultRow.toCategory() = Category(
        id = this[Categories.id].value,
        name = this[Categories.name],
        slug = this[Categories.slug],
        description = this[Categories.description],
        icon = this[Categories.icon],
        parentId = this[Categories.parentId]?.value,
        sortOrder = this[Categories.sortOrder],
        createdAt = this[Categories.createdAt]
    )
}
