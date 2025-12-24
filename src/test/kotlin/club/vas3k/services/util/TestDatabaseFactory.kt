package club.vas3k.services.util

import club.vas3k.services.database.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object TestDatabaseFactory {

    fun init() {
        val database = Database.connect(
            url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )

        transaction(database) {
            SchemaUtils.create(Users, Categories, Services, ServiceCategories)
        }
    }

    fun cleanUp() {
        transaction {
            SchemaUtils.drop(ServiceCategories, Services, Categories, Users)
        }
    }
}
