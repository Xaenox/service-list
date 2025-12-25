package club.vas3k.services.auth

import club.vas3k.services.domain.User
import club.vas3k.services.domain.toProfileResponse
import club.vas3k.services.repository.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import java.util.*

@Serializable
data class OAuthState(val state: String, val returnUrl: String? = null)

@Serializable
data class TokenResponse(val token: String, val expiresIn: Int)

@Serializable
data class ErrorResponse(val error: String, val message: String)

fun Route.authRoutes() {
    val oauthClient by inject<Vas3kOAuthClient>()
    val jwtService by inject<JwtService>()
    val userRepository by inject<UserRepository>()

    route("/auth") {
        get("/login") {
            val returnUrl = call.request.queryParameters["return_url"]
            val state = UUID.randomUUID().toString()

            call.sessions.set(OAuthState(state, returnUrl))

            val authUrl = oauthClient.getAuthorizationUrl(state)
            call.respondRedirect(authUrl)
        }

        get("/callback") {
            val code = call.request.queryParameters["code"]
            val state = call.request.queryParameters["state"]
            val savedState = call.sessions.get<OAuthState>()

            if (code == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_request", "Missing authorization code"))
                return@get
            }

            if (state == null || savedState == null || state != savedState.state) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_state", "Invalid state parameter"))
                return@get
            }

            try {
                val tokenResponse = oauthClient.exchangeCodeForToken(code)
                val userInfo = oauthClient.getUserInfo(tokenResponse.accessToken)

                if (userInfo.paymentStatus != "active") {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ErrorResponse("inactive_membership", "Only active club members can access this service")
                    )
                    return@get
                }

                val user = userRepository.upsertFromOAuth(
                    slug = userInfo.sub,
                    email = userInfo.email,
                    fullName = userInfo.fullName ?: userInfo.sub,
                    avatarUrl = userInfo.avatar,
                    country = userInfo.country,
                    city = userInfo.city
                )

                val jwt = jwtService.generateToken(user)

                call.sessions.clear<OAuthState>()

                val returnUrl = savedState.returnUrl
                if (returnUrl != null) {
                    call.respondRedirect("$returnUrl?token=$jwt")
                } else {
                    call.respond(TokenResponse(jwt, 30 * 24 * 60 * 60))
                }

            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("oauth_error", "Failed to authenticate: ${e.message}")
                )
            }
        }

        authenticate("jwt") {
            get("/me") {
                val user = call.principal<User>()
                if (user == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("unauthorized", "Not authenticated"))
                    return@get
                }
                call.respond(user.toProfileResponse())
            }
        }
    }
}
