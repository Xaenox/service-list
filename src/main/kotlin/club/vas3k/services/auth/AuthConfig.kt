package club.vas3k.services.auth

import club.vas3k.services.domain.User
import club.vas3k.services.domain.UserRole
import club.vas3k.services.repository.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import org.koin.ktor.ext.inject

fun Application.configureAuth() {
    val jwtService by inject<JwtService>()
    val userRepository by inject<UserRepository>()

    val jwtRealm = environment.config.property("jwt.realm").getString()

    install(Sessions) {
        cookie<OAuthState>("oauth_state") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 600
            cookie.httpOnly = true
        }
    }

    install(Authentication) {
        jwt("jwt") {
            realm = jwtRealm
            verifier(jwtService.verifier)

            validate { credential ->
                val userId = jwtService.getUserIdFromToken(credential.payload)
                if (userId != null) {
                    userRepository.findById(userId)
                } else {
                    null
                }
            }

            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("unauthorized", "Token is invalid or expired")
                )
            }
        }

        jwt("jwt-moderator") {
            realm = jwtRealm
            verifier(jwtService.verifier)

            validate { credential ->
                val userId = jwtService.getUserIdFromToken(credential.payload)
                val role = jwtService.getRoleFromToken(credential.payload)

                if (userId != null && (role == UserRole.MODERATOR.name || role == UserRole.ADMIN.name)) {
                    userRepository.findById(userId)
                } else {
                    null
                }
            }

            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Forbidden,
                    ErrorResponse("forbidden", "Moderator access required")
                )
            }
        }

        jwt("jwt-admin") {
            realm = jwtRealm
            verifier(jwtService.verifier)

            validate { credential ->
                val userId = jwtService.getUserIdFromToken(credential.payload)
                val role = jwtService.getRoleFromToken(credential.payload)

                if (userId != null && role == UserRole.ADMIN.name) {
                    userRepository.findById(userId)
                } else {
                    null
                }
            }

            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Forbidden,
                    ErrorResponse("forbidden", "Admin access required")
                )
            }
        }

        jwt("jwt-optional") {
            realm = jwtRealm
            verifier(jwtService.verifier)

            validate { credential ->
                val userId = jwtService.getUserIdFromToken(credential.payload)
                if (userId != null) {
                    userRepository.findById(userId)
                } else {
                    null
                }
            }
        }
    }
}
