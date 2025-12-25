package club.vas3k.services.routes

import club.vas3k.services.module
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ApplicationTest {

    private fun createTestConfig() = MapApplicationConfig().apply {
        // Ktor settings
        put("ktor.deployment.port", "8080")

        // JWT settings
        put("jwt.secret", "test-secret-key-for-testing-only-at-least-256-bits-long")
        put("jwt.issuer", "http://localhost:8080/")
        put("jwt.audience", "http://localhost:8080/api")
        put("jwt.realm", "vas3k-services")
        put("jwt.expirationDays", "30")

        // OAuth settings
        put("oauth.vas3k.clientId", "test-client-id")
        put("oauth.vas3k.clientSecret", "test-client-secret")
        put("oauth.vas3k.authorizeUrl", "https://vas3k.club/auth/authorize/")
        put("oauth.vas3k.accessTokenUrl", "https://vas3k.club/auth/token/")
        put("oauth.vas3k.userInfoUrl", "https://vas3k.club/auth/me/")
        put("oauth.vas3k.redirectUrl", "http://localhost:8080/auth/callback")

        // Database settings
        put("database.url", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL")
        put("database.driver", "org.h2.Driver")
        put("database.user", "sa")
        put("database.password", "")
        put("database.maxPoolSize", "5")
    }

    @Test
    fun `health endpoint should return OK`() = testApplication {
        environment {
            config = createTestConfig()
        }
        application {
            module()
        }

        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("{\"status\":\"ok\"}", response.bodyAsText())
    }

    @Test
    fun `root endpoint should return 404`() = testApplication {
        environment {
            config = createTestConfig()
        }
        application {
            module()
        }

        val response = client.get("/")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
