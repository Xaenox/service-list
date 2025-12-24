package club.vas3k.services.plugins

import club.vas3k.services.auth.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.SerializationException
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("StatusPages")

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<SerializationException> { call, cause ->
            logger.warn("Serialization error: ${cause.message}")
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("invalid_json", "Invalid request body: ${cause.message}")
            )
        }

        exception<IllegalArgumentException> { call, cause ->
            logger.warn("Validation error: ${cause.message}")
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("validation_error", cause.message ?: "Invalid request")
            )
        }

        exception<Throwable> { call, cause ->
            logger.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("internal_error", "An unexpected error occurred")
            )
        }

        status(HttpStatusCode.NotFound) { call, _ ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse("not_found", "The requested resource was not found")
            )
        }

        status(HttpStatusCode.MethodNotAllowed) { call, _ ->
            call.respond(
                HttpStatusCode.MethodNotAllowed,
                ErrorResponse("method_not_allowed", "Method not allowed for this resource")
            )
        }
    }
}
