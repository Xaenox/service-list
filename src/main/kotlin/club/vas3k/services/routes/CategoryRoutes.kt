package club.vas3k.services.routes

import club.vas3k.services.auth.ErrorResponse
import club.vas3k.services.domain.*
import club.vas3k.services.repository.CategoryRepository
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.util.*

fun Route.categoryRoutes() {
    val categoryRepository by inject<CategoryRepository>()

    route("/categories") {
        get {
            val flat = call.request.queryParameters["flat"]?.toBooleanStrictOrNull() ?: false

            if (flat) {
                val categories = categoryRepository.findAll()
                call.respond(categories.map { it.toResponse() })
            } else {
                val rootCategories = categoryRepository.findRootCategories()
                val allCategories = categoryRepository.findAll()

                val categoryTree = rootCategories.map { root ->
                    val children = allCategories
                        .filter { it.parentId == root.id }
                        .map { it.toResponse() }
                    root.toResponse(children)
                }

                call.respond(categoryTree)
            }
        }

        get("/{slug}") {
            val slug = call.parameters["slug"]

            if (slug.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_slug", "Slug is required"))
                return@get
            }

            val category = categoryRepository.findBySlug(slug)
            if (category == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("not_found", "Category not found"))
                return@get
            }

            val children = categoryRepository.findChildren(category.id)
            call.respond(category.toResponse(children.map { it.toResponse() }))
        }

        authenticate("jwt-admin") {
            post {
                val request = call.receive<CreateCategoryRequest>()

                if (request.name.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("validation_error", "Name is required"))
                    return@post
                }

                if (request.slug.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("validation_error", "Slug is required"))
                    return@post
                }

                if (!request.slug.matches(Regex("^[a-z0-9-]+$"))) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("validation_error", "Slug must contain only lowercase letters, numbers, and hyphens"))
                    return@post
                }

                if (categoryRepository.existsBySlug(request.slug)) {
                    call.respond(HttpStatusCode.Conflict, ErrorResponse("conflict", "Category with this slug already exists"))
                    return@post
                }

                val parentId = request.parentId?.let {
                    runCatching { UUID.fromString(it) }.getOrNull()
                }

                if (parentId != null && categoryRepository.findById(parentId) == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("validation_error", "Parent category not found"))
                    return@post
                }

                val category = categoryRepository.create(
                    name = request.name.trim(),
                    slug = request.slug.trim().lowercase(),
                    description = request.description?.trim(),
                    icon = request.icon?.trim(),
                    parentId = parentId,
                    sortOrder = request.sortOrder
                )

                call.respond(HttpStatusCode.Created, category.toResponse())
            }

            put("/{id}") {
                val id = call.parameters["id"]?.let {
                    runCatching { UUID.fromString(it) }.getOrNull()
                }

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_id", "Invalid category ID format"))
                    return@put
                }

                val existing = categoryRepository.findById(id)
                if (existing == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("not_found", "Category not found"))
                    return@put
                }

                val request = call.receive<UpdateCategoryRequest>()

                val updated = categoryRepository.update(
                    id = id,
                    name = request.name?.trim(),
                    description = request.description?.trim(),
                    icon = request.icon?.trim(),
                    sortOrder = request.sortOrder
                )

                if (!updated) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("update_failed", "Failed to update category"))
                    return@put
                }

                val updatedCategory = categoryRepository.findById(id)!!
                call.respond(updatedCategory.toResponse())
            }

            delete("/{id}") {
                val id = call.parameters["id"]?.let {
                    runCatching { UUID.fromString(it) }.getOrNull()
                }

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_id", "Invalid category ID format"))
                    return@delete
                }

                val children = categoryRepository.findChildren(id)
                if (children.isNotEmpty()) {
                    call.respond(HttpStatusCode.Conflict, ErrorResponse("has_children", "Cannot delete category with subcategories"))
                    return@delete
                }

                val deleted = categoryRepository.delete(id)
                if (!deleted) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("not_found", "Category not found"))
                    return@delete
                }

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
