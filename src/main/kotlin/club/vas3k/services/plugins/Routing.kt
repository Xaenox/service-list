package club.vas3k.services.plugins

import club.vas3k.services.auth.authRoutes
import club.vas3k.services.routes.categoryRoutes
import club.vas3k.services.routes.serviceRoutes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val version: String
)

fun Application.configureRouting() {
    routing {
        get("/health") {
            call.respond(HealthResponse("ok", "1.0.0"))
        }

        route("/api/v1") {
            authRoutes()
            serviceRoutes()
            categoryRoutes()
        }
    }
}
