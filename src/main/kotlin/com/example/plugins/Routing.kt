package com.example.plugins
import Conexion
import Pedido
import io.ktor.server.routing.*
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

fun Application.configureRouting() {
    routing {
        get("/") {
            Conexion.conectar()
            transaction {
                val clienteId = Cliente.findById(1)?.id
                val nuevoPedido = Pedido.new {
                        id_cliente = clienteId!!
                        fecha_pedido = LocalDate.now()
                        estado = Pedidos.EstadoPedido.PENDIENTE
                }
            }

        }
    }
}
