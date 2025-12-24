package club.vas3k.services.auth

import club.vas3k.services.domain.User
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import java.util.*

class JwtService(
    private val secret: String,
    private val issuer: String,
    private val audience: String,
    private val expirationDays: Int
) {
    private val algorithm = Algorithm.HMAC256(secret)

    val verifier = JWT.require(algorithm)
        .withAudience(audience)
        .withIssuer(issuer)
        .build()

    fun generateToken(user: User): String {
        return JWT.create()
            .withSubject(user.id.toString())
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("slug", user.slug)
            .withClaim("email", user.email)
            .withClaim("role", user.role.name)
            .withExpiresAt(Date(System.currentTimeMillis() + expirationDays * 24 * 60 * 60 * 1000L))
            .sign(algorithm)
    }

    fun getUserIdFromToken(token: DecodedJWT): UUID? {
        return runCatching { UUID.fromString(token.subject) }.getOrNull()
    }

    fun getRoleFromToken(token: DecodedJWT): String? {
        return token.getClaim("role").asString()
    }
}
