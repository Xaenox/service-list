package club.vas3k.services.routes

import club.vas3k.services.auth.ErrorResponse
import club.vas3k.services.domain.*
import club.vas3k.services.repository.ServiceRepository
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.util.*

fun Route.serviceRoutes() {
    val serviceRepository by inject<ServiceRepository>()

    route("/services") {
        get {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull()?.coerceIn(1, 50) ?: 20

            val filter = ServiceFilter(
                query = call.request.queryParameters["q"],
                categorySlug = call.request.queryParameters["category"],
                type = call.request.queryParameters["type"]?.let {
                    runCatching { ServiceType.valueOf(it.uppercase()) }.getOrNull()
                },
                city = call.request.queryParameters["city"],
                country = call.request.queryParameters["country"],
                hasBonus = call.request.queryParameters["hasBonus"]?.toBooleanStrictOrNull(),
                ownerId = call.request.queryParameters["ownerId"]
            )

            val (services, totalCount) = serviceRepository.findAll(filter, page, pageSize)
            val response = createPagedResponse(
                items = services.map { it.toListItemResponse() },
                page = page,
                pageSize = pageSize,
                totalItems = totalCount
            )

            call.respond(response)
        }

        get("/{id}") {
            val id = call.parameters["id"]?.let {
                runCatching { UUID.fromString(it) }.getOrNull()
            }

            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_id", "Invalid service ID format"))
                return@get
            }

            val service = serviceRepository.findById(id)
            if (service == null || service.service.status == ServiceStatus.DELETED) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("not_found", "Service not found"))
                return@get
            }

            call.respond(service.toResponse())
        }

        authenticate("jwt") {
            post {
                val user = call.principal<User>()!!
                val request = call.receive<CreateServiceRequest>()

                if (request.title.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("validation_error", "Title is required"))
                    return@post
                }

                if (request.description.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("validation_error", "Description is required"))
                    return@post
                }

                val categoryIds = request.categoryIds.mapNotNull {
                    runCatching { UUID.fromString(it) }.getOrNull()
                }

                val service = serviceRepository.create(
                    title = request.title.trim(),
                    description = request.description.trim(),
                    type = request.type,
                    contacts = request.contacts,
                    location = request.location,
                    bonus = request.bonus,
                    tags = request.tags.map { it.trim().lowercase() },
                    ownerId = user.id,
                    categoryIds = categoryIds
                )

                if (service == null) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("creation_failed", "Failed to create service"))
                    return@post
                }

                call.respond(HttpStatusCode.Created, service.toResponse())
            }

            put("/{id}") {
                val user = call.principal<User>()!!
                val id = call.parameters["id"]?.let {
                    runCatching { UUID.fromString(it) }.getOrNull()
                }

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_id", "Invalid service ID format"))
                    return@put
                }

                val existingService = serviceRepository.findById(id)
                if (existingService == null || existingService.service.status == ServiceStatus.DELETED) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("not_found", "Service not found"))
                    return@put
                }

                val isOwner = serviceRepository.isOwner(id, user.id)
                val isModerator = user.role == UserRole.MODERATOR || user.role == UserRole.ADMIN

                if (!isOwner && !isModerator) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("forbidden", "You can only edit your own services"))
                    return@put
                }

                val request = call.receive<UpdateServiceRequest>()

                val categoryIds = request.categoryIds?.mapNotNull {
                    runCatching { UUID.fromString(it) }.getOrNull()
                }

                val updated = serviceRepository.update(
                    id = id,
                    title = request.title?.trim(),
                    description = request.description?.trim(),
                    type = request.type,
                    status = if (isModerator) request.status else null,
                    contacts = request.contacts,
                    location = request.location,
                    bonus = request.bonus,
                    tags = request.tags?.map { it.trim().lowercase() },
                    categoryIds = categoryIds
                )

                if (!updated) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("update_failed", "Failed to update service"))
                    return@put
                }

                val updatedService = serviceRepository.findById(id)!!
                call.respond(updatedService.toResponse())
            }

            delete("/{id}") {
                val user = call.principal<User>()!!
                val id = call.parameters["id"]?.let {
                    runCatching { UUID.fromString(it) }.getOrNull()
                }

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_id", "Invalid service ID format"))
                    return@delete
                }

                val isOwner = serviceRepository.isOwner(id, user.id)
                val isModerator = user.role == UserRole.MODERATOR || user.role == UserRole.ADMIN

                if (!isOwner && !isModerator) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("forbidden", "You can only delete your own services"))
                    return@delete
                }

                val deleted = serviceRepository.delete(id)
                if (!deleted) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("not_found", "Service not found"))
                    return@delete
                }

                call.respond(HttpStatusCode.NoContent)
            }
        }

        authenticate("jwt-moderator") {
            delete("/{id}/hard") {
                val id = call.parameters["id"]?.let {
                    runCatching { UUID.fromString(it) }.getOrNull()
                }

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_id", "Invalid service ID format"))
                    return@delete
                }

                val deleted = serviceRepository.hardDelete(id)
                if (!deleted) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("not_found", "Service not found"))
                    return@delete
                }

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }

    route("/users/{userId}/services") {
        get {
            val userId = call.parameters["userId"]?.let {
                runCatching { UUID.fromString(it) }.getOrNull()
            }

            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_id", "Invalid user ID format"))
                return@get
            }

            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull()?.coerceIn(1, 50) ?: 20

            val (services, totalCount) = serviceRepository.findByOwnerId(userId, page, pageSize)
            val response = createPagedResponse(
                items = services.map { it.toListItemResponse() },
                page = page,
                pageSize = pageSize,
                totalItems = totalCount
            )

            call.respond(response)
        }
    }

    authenticate("jwt") {
        get("/my/services") {
            val user = call.principal<User>()!!

            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull()?.coerceIn(1, 50) ?: 20

            val (services, totalCount) = serviceRepository.findByOwnerId(user.id, page, pageSize)
            val response = createPagedResponse(
                items = services.map { it.toListItemResponse() },
                page = page,
                pageSize = pageSize,
                totalItems = totalCount
            )

            call.respond(response)
        }
    }
}
