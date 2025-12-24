package club.vas3k.services.auth

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Vas3kTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Int? = null,
    @SerialName("refresh_token") val refreshToken: String? = null
)

@Serializable
data class Vas3kUserInfo(
    val sub: String,
    val email: String,
    @SerialName("full_name") val fullName: String? = null,
    val avatar: String? = null,
    val country: String? = null,
    val city: String? = null,
    @SerialName("payment_status") val paymentStatus: String? = null
)

class Vas3kOAuthClient(
    private val clientId: String,
    private val clientSecret: String,
    private val authorizeUrl: String,
    private val accessTokenUrl: String,
    private val userInfoUrl: String,
    private val redirectUrl: String
) {
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    fun getAuthorizationUrl(state: String): String {
        return URLBuilder(authorizeUrl).apply {
            parameters.append("client_id", clientId)
            parameters.append("redirect_uri", redirectUrl)
            parameters.append("response_type", "code")
            parameters.append("state", state)
            parameters.append("scope", "openid")
        }.buildString()
    }

    suspend fun exchangeCodeForToken(code: String): Vas3kTokenResponse {
        return httpClient.submitForm(
            url = accessTokenUrl,
            formParameters = parameters {
                append("grant_type", "authorization_code")
                append("client_id", clientId)
                append("client_secret", clientSecret)
                append("code", code)
                append("redirect_uri", redirectUrl)
            }
        ).body()
    }

    suspend fun getUserInfo(accessToken: String): Vas3kUserInfo {
        return httpClient.get(userInfoUrl) {
            bearerAuth(accessToken)
        }.body()
    }

    fun close() {
        httpClient.close()
    }
}
