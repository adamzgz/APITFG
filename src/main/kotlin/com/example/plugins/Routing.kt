package com.example.plugins
import Conexion
import Pedido
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

fun Application.configureRouting() {
    routing {
        get("/") {
                call.respond(HttpStatusCode.BadRequest,"ERROR")
            }

    }
}
