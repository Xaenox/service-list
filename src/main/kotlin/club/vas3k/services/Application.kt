package club.vas3k.services

import club.vas3k.services.auth.configureAuth
import club.vas3k.services.database.DatabaseConfig
import club.vas3k.services.di.appModule
import club.vas3k.services.plugins.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    install(Koin) {
        slf4jLogger()
        modules(appModule(environment))
    }

    DatabaseConfig.init(environment)

    configureLogging()
    configureSerialization()
    configureHTTP()
    configureStatusPages()
    configureAuth()
    configureRouting()

    val port = runCatching {
        environment.config.property("ktor.deployment.port").getString()
    }.getOrElse { "unknown" }
    log.info("Community Services API started on port $port")
}
