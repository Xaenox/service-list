package club.vas3k.services.di

import club.vas3k.services.auth.JwtService
import club.vas3k.services.auth.Vas3kOAuthClient
import club.vas3k.services.repository.CategoryRepository
import club.vas3k.services.repository.ServiceRepository
import club.vas3k.services.repository.UserRepository
import io.ktor.server.application.*
import org.koin.dsl.module

fun appModule(environment: ApplicationEnvironment) = module {
    single {
        Vas3kOAuthClient(
            clientId = environment.config.property("oauth.vas3k.clientId").getString(),
            clientSecret = environment.config.property("oauth.vas3k.clientSecret").getString(),
            authorizeUrl = environment.config.property("oauth.vas3k.authorizeUrl").getString(),
            accessTokenUrl = environment.config.property("oauth.vas3k.accessTokenUrl").getString(),
            userInfoUrl = environment.config.property("oauth.vas3k.userInfoUrl").getString(),
            redirectUrl = environment.config.property("oauth.vas3k.redirectUrl").getString()
        )
    }

    single {
        JwtService(
            secret = environment.config.property("jwt.secret").getString(),
            issuer = environment.config.property("jwt.issuer").getString(),
            audience = environment.config.property("jwt.audience").getString(),
            expirationDays = environment.config.property("jwt.expirationDays").getString().toInt()
        )
    }

    single { UserRepository() }
    single { CategoryRepository() }
    single { ServiceRepository(get(), get()) }
}
