package club.vas3k.services.repository

import club.vas3k.services.database.*
import club.vas3k.services.domain.*
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import java.util.UUID

class ServiceRepository(
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun findById(id: UUID): ServiceWithDetails? = dbQuery {
        val serviceRow = Services.selectAll().where { Services.id eq id }
            .singleOrNull() ?: return@dbQuery null

        val service = serviceRow.toService()
        val owner = userRepository.findById(service.ownerId) ?: return@dbQuery null
        val categories = getServiceCategories(id)

        ServiceWithDetails(service, owner, categories)
    }

    suspend fun findAll(
        filter: ServiceFilter,
        page: Int,
        pageSize: Int
    ): Pair<List<ServiceWithDetails>, Long> = dbQuery {
        val query = buildFilterQuery(filter)

        val totalCount = query.count()

        val services = query
            .orderBy(Services.createdAt to SortOrder.DESC)
            .limit(pageSize, ((page - 1) * pageSize).toLong())
            .map { it.toService() }

        val servicesWithDetails = services.mapNotNull { service ->
            val owner = userRepository.findById(service.ownerId) ?: return@mapNotNull null
            val categories = getServiceCategories(service.id)
            ServiceWithDetails(service, owner, categories)
        }

        servicesWithDetails to totalCount
    }

    suspend fun findByOwnerId(ownerId: UUID, page: Int, pageSize: Int): Pair<List<ServiceWithDetails>, Long> = dbQuery {
        val query = Services.selectAll().where { Services.ownerId eq ownerId }

        val totalCount = query.count()

        val services = query
            .orderBy(Services.createdAt to SortOrder.DESC)
            .limit(pageSize, ((page - 1) * pageSize).toLong())
            .map { it.toService() }

        val owner = userRepository.findById(ownerId)
        if (owner == null) {
            emptyList<ServiceWithDetails>() to 0L
        } else {
            val servicesWithDetails = services.map { service ->
                val categories = getServiceCategories(service.id)
                ServiceWithDetails(service, owner, categories)
            }
            servicesWithDetails to totalCount
        }
    }

    suspend fun create(
        title: String,
        description: String,
        type: ServiceType,
        contacts: Contacts,
        location: Location?,
        bonus: Bonus?,
        tags: List<String>,
        ownerId: UUID,
        categoryIds: List<UUID>
    ): ServiceWithDetails? = dbQuery {
        val now = Clock.System.now()
        val id = UUID.randomUUID()

        Services.insert {
            it[Services.id] = id
            it[Services.title] = title
            it[Services.description] = description
            it[Services.type] = type
            it[Services.status] = ServiceStatus.ACTIVE
            it[Services.contactsJson] = json.encodeToString(contacts)
            it[Services.locationCountry] = location?.country
            it[Services.locationCity] = location?.city
            it[Services.locationAddress] = location?.address
            it[Services.locationLatitude] = location?.latitude
            it[Services.locationLongitude] = location?.longitude
            it[Services.bonusJson] = bonus?.let { b -> json.encodeToString(b) }
            it[Services.tagsJson] = json.encodeToString(tags)
            it[Services.ownerId] = ownerId
            it[Services.createdAt] = now
            it[Services.updatedAt] = now
        }

        categoryIds.forEach { categoryId ->
            ServiceCategories.insert {
                it[ServiceCategories.serviceId] = id
                it[ServiceCategories.categoryId] = categoryId
            }
        }

        val owner = userRepository.findById(ownerId) ?: return@dbQuery null
        val categories = categoryRepository.findByIds(categoryIds)

        ServiceWithDetails(
            service = Service(
                id = id,
                title = title,
                description = description,
                type = type,
                status = ServiceStatus.ACTIVE,
                contacts = contacts,
                location = location,
                bonus = bonus,
                tags = tags,
                ownerId = ownerId,
                createdAt = now,
                updatedAt = now
            ),
            owner = owner,
            categories = categories
        )
    }

    suspend fun update(
        id: UUID,
        title: String?,
        description: String?,
        type: ServiceType?,
        status: ServiceStatus?,
        contacts: Contacts?,
        location: Location?,
        bonus: Bonus?,
        tags: List<String>?,
        categoryIds: List<UUID>?
    ): Boolean = dbQuery {
        val now = Clock.System.now()

        val updated = Services.update({ Services.id eq id }) {
            title?.let { t -> it[Services.title] = t }
            description?.let { d -> it[Services.description] = d }
            type?.let { t -> it[Services.type] = t }
            status?.let { s -> it[Services.status] = s }
            contacts?.let { c -> it[Services.contactsJson] = json.encodeToString(c) }
            location?.let { l ->
                it[Services.locationCountry] = l.country
                it[Services.locationCity] = l.city
                it[Services.locationAddress] = l.address
                it[Services.locationLatitude] = l.latitude
                it[Services.locationLongitude] = l.longitude
            }
            bonus?.let { b -> it[Services.bonusJson] = json.encodeToString(b) }
            tags?.let { t -> it[Services.tagsJson] = json.encodeToString(t) }
            it[Services.updatedAt] = now
        } > 0

        if (updated && categoryIds != null) {
            ServiceCategories.deleteWhere { ServiceCategories.serviceId eq id }
            categoryIds.forEach { categoryId ->
                ServiceCategories.insert {
                    it[ServiceCategories.serviceId] = id
                    it[ServiceCategories.categoryId] = categoryId
                }
            }
        }

        updated
    }

    suspend fun delete(id: UUID): Boolean = dbQuery {
        Services.update({ Services.id eq id }) {
            it[Services.status] = ServiceStatus.DELETED
            it[Services.updatedAt] = Clock.System.now()
        } > 0
    }

    suspend fun hardDelete(id: UUID): Boolean = dbQuery {
        ServiceCategories.deleteWhere { ServiceCategories.serviceId eq id }
        Services.deleteWhere { Services.id eq id } > 0
    }

    suspend fun isOwner(serviceId: UUID, userId: UUID): Boolean = dbQuery {
        Services.selectAll().where { (Services.id eq serviceId) and (Services.ownerId eq userId) }
            .count() > 0
    }

    private fun buildFilterQuery(filter: ServiceFilter): Query {
        val conditions = mutableListOf<Op<Boolean>>()

        conditions.add(Services.status eq filter.status)

        filter.query?.let { q ->
            conditions.add(
                with(SqlExpressionBuilder) {
                    (Services.title.lowerCase() like "%${q.lowercase()}%") or
                            (Services.description.lowerCase() like "%${q.lowercase()}%")
                }
            )
        }

        filter.type?.let { t ->
            conditions.add(Services.type eq t)
        }

        filter.city?.let { c ->
            conditions.add(Services.locationCity.lowerCase() eq c.lowercase())
        }

        filter.country?.let { c ->
            conditions.add(Services.locationCountry.lowerCase() eq c.lowercase())
        }

        filter.hasBonus?.let { has ->
            if (has) {
                conditions.add(with(SqlExpressionBuilder) { Services.bonusJson.isNotNull() })
            } else {
                conditions.add(with(SqlExpressionBuilder) { Services.bonusJson.isNull() })
            }
        }

        filter.ownerId?.let { oid ->
            runCatching { UUID.fromString(oid) }.getOrNull()?.let { uuid ->
                conditions.add(Services.ownerId eq uuid)
            }
        }

        var query = Services.selectAll()

        filter.categorySlug?.let { slug ->
            query = Services.innerJoin(ServiceCategories)
                .innerJoin(Categories)
                .selectAll()
                .where { Categories.slug eq slug }
        }

        return if (conditions.isNotEmpty()) {
            val combined = conditions.reduce { acc, op -> acc and op }
            if (filter.categorySlug != null) {
                query.andWhere { combined }
            } else {
                query.where { combined }
            }
        } else {
            query
        }
    }

    private suspend fun getServiceCategories(serviceId: UUID): List<Category> {
        val categoryIds = ServiceCategories.selectAll()
            .where { ServiceCategories.serviceId eq serviceId }
            .map { it[ServiceCategories.categoryId].value }

        return categoryRepository.findByIds(categoryIds)
    }

    private fun ResultRow.toService(): Service {
        val location = if (this[Services.locationCountry] != null || this[Services.locationCity] != null) {
            Location(
                country = this[Services.locationCountry],
                city = this[Services.locationCity],
                address = this[Services.locationAddress],
                latitude = this[Services.locationLatitude],
                longitude = this[Services.locationLongitude]
            )
        } else null

        return Service(
            id = this[Services.id].value,
            title = this[Services.title],
            description = this[Services.description],
            type = this[Services.type],
            status = this[Services.status],
            contacts = json.decodeFromString(this[Services.contactsJson]),
            location = location,
            bonus = this[Services.bonusJson]?.let { json.decodeFromString(it) },
            tags = json.decodeFromString(this[Services.tagsJson]),
            ownerId = this[Services.ownerId].value,
            createdAt = this[Services.createdAt],
            updatedAt = this[Services.updatedAt]
        )
    }
}
