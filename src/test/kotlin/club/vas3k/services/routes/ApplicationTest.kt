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

    @Test
    fun `health endpoint should return OK`() = testApplication {
        environment {
            config = ApplicationConfig("application-test.yaml")
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
            config = ApplicationConfig("application-test.yaml")
        }
        application {
            module()
        }

        val response = client.get("/")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
