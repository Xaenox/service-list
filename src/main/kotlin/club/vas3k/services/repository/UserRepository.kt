package club.vas3k.services.repository

import club.vas3k.services.database.Users
import club.vas3k.services.database.dbQuery
import club.vas3k.services.domain.User
import club.vas3k.services.domain.UserRole
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.UUID

class UserRepository {

    suspend fun findById(id: UUID): User? = dbQuery {
        Users.selectAll().where { Users.id eq id }
            .map { it.toUser() }
            .singleOrNull()
    }

    suspend fun findBySlug(slug: String): User? = dbQuery {
        Users.selectAll().where { Users.slug eq slug }
            .map { it.toUser() }
            .singleOrNull()
    }

    suspend fun findByEmail(email: String): User? = dbQuery {
        Users.selectAll().where { Users.email eq email }
            .map { it.toUser() }
            .singleOrNull()
    }

    suspend fun create(
        slug: String,
        email: String,
        fullName: String,
        avatarUrl: String?,
        country: String?,
        city: String?
    ): User = dbQuery {
        val now = Clock.System.now()
        val id = UUID.randomUUID()

        Users.insert {
            it[Users.id] = id
            it[Users.slug] = slug
            it[Users.email] = email
            it[Users.fullName] = fullName
            it[Users.avatarUrl] = avatarUrl
            it[Users.country] = country
            it[Users.city] = city
            it[Users.role] = UserRole.USER
            it[Users.isActive] = true
            it[Users.createdAt] = now
            it[Users.updatedAt] = now
        }

        User(
            id = id,
            slug = slug,
            email = email,
            fullName = fullName,
            avatarUrl = avatarUrl,
            country = country,
            city = city,
            role = UserRole.USER,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )
    }

    suspend fun update(
        id: UUID,
        fullName: String? = null,
        avatarUrl: String? = null,
        country: String? = null,
        city: String? = null
    ): Boolean = dbQuery {
        val now = Clock.System.now()

        Users.update({ Users.id eq id }) {
            fullName?.let { name -> it[Users.fullName] = name }
            avatarUrl?.let { url -> it[Users.avatarUrl] = url }
            country?.let { c -> it[Users.country] = c }
            city?.let { c -> it[Users.city] = c }
            it[Users.updatedAt] = now
        } > 0
    }

    suspend fun updateRole(id: UUID, role: UserRole): Boolean = dbQuery {
        Users.update({ Users.id eq id }) {
            it[Users.role] = role
            it[Users.updatedAt] = Clock.System.now()
        } > 0
    }

    suspend fun upsertFromOAuth(
        slug: String,
        email: String,
        fullName: String,
        avatarUrl: String?,
        country: String?,
        city: String?
    ): User = dbQuery {
        val existing = Users.selectAll().where { Users.slug eq slug }
            .map { it.toUser() }
            .singleOrNull()

        if (existing != null) {
            val now = Clock.System.now()
            Users.update({ Users.slug eq slug }) {
                it[Users.email] = email
                it[Users.fullName] = fullName
                it[Users.avatarUrl] = avatarUrl
                it[Users.country] = country
                it[Users.city] = city
                it[Users.updatedAt] = now
            }
            existing.copy(
                email = email,
                fullName = fullName,
                avatarUrl = avatarUrl,
                country = country,
                city = city,
                updatedAt = now
            )
        } else {
            val now = Clock.System.now()
            val id = UUID.randomUUID()

            Users.insert {
                it[Users.id] = id
                it[Users.slug] = slug
                it[Users.email] = email
                it[Users.fullName] = fullName
                it[Users.avatarUrl] = avatarUrl
                it[Users.country] = country
                it[Users.city] = city
                it[Users.role] = UserRole.USER
                it[Users.isActive] = true
                it[Users.createdAt] = now
                it[Users.updatedAt] = now
            }

            User(
                id = id,
                slug = slug,
                email = email,
                fullName = fullName,
                avatarUrl = avatarUrl,
                country = country,
                city = city,
                role = UserRole.USER,
                isActive = true,
                createdAt = now,
                updatedAt = now
            )
        }
    }

    private fun ResultRow.toUser() = User(
        id = this[Users.id].value,
        slug = this[Users.slug],
        email = this[Users.email],
        fullName = this[Users.fullName],
        avatarUrl = this[Users.avatarUrl],
        country = this[Users.country],
        city = this[Users.city],
        role = this[Users.role],
        isActive = this[Users.isActive],
        createdAt = this[Users.createdAt],
        updatedAt = this[Users.updatedAt]
    )
}
