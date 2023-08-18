import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

object Pedidos : IntIdTable() {
    val id_cliente: Column<EntityID<Int>> = reference("id_cliente", Clientes)
    val estado: Column<EstadoPedido> = enumerationByName("estado", 50, EstadoPedido::class)
    val fecha_pedido: Column<LocalDate> = date("fecha_pedido")

    enum class EstadoPedido {

        PENDIENTE,
        ENVIADO,
        ENTREGADO,
        EN_PROCESO
    }
}

class Pedido(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Pedido>(Pedidos) {
        @Serializable
        data class PedidoDto(
            val idCliente: Int,
            val estado: String
        )

        fun crearPedido(idCliente: Int, estado: Pedidos.EstadoPedido): Boolean {
            if (idCliente <= 0) {
                println("ID de cliente no válido.")
                return false
            }

            return transaction {
                try {
                    // Verificar si el ID de cliente existe
                    val cliente = Cliente.findById(EntityID(idCliente, Clientes))
                    if (cliente == null) {
                        println("El cliente con ID $idCliente no existe.")
                        return@transaction false
                    }

                    // Crear el nuevo pedido
                    Pedido.new {
                        this.id_cliente = cliente.id
                        this.estado = estado
                        this.fecha_pedido = LocalDate.now()
                    }

                    return@transaction true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@transaction false
                }
            }
        }

        fun borrarPedido(idPedido: Int): Boolean {
            if (idPedido <= 0) {
                println("ID de pedido no válido.")
                return false
            }

            return transaction {
                try {
                    // Buscar el pedido por su ID
                    val pedido = Pedido.findById(idPedido)
                    if (pedido == null) {
                        println("El pedido con ID $idPedido no existe.")
                        return@transaction false
                    }

                    // Eliminar el pedido
                    pedido.delete()

                    return@transaction true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@transaction false
                }
            }
        }
        fun obtenerPedidosPorUsuario(idUsuario: Int): List<PedidoDto> {
            return transaction {
                val cliente = Cliente.findById(idUsuario)
                if (cliente != null) {
                    return@transaction Pedido.find { Pedidos.id_cliente eq cliente.id }.map { pedido ->
                        PedidoDto(
                            pedido.id_cliente.value,
                            pedido.estado.name
                        )
                    }
                } else {
                    return@transaction emptyList()
                }
            }
        }

        fun actualizarPedido(
            idPedido: Int,
            nuevoEstado: Pedidos.EstadoPedido? = null
        ): Boolean {
            if (idPedido <= 0 || nuevoEstado == null) {
                println("Datos ingresados no válidos.")
                return false
            }

            return transaction {
                try {
                    // Buscar el pedido por su ID
                    val pedido = Pedido.findById(idPedido)
                    if (pedido == null) {
                        println("El pedido con ID $idPedido no existe.")
                        return@transaction false
                    }

                    // Verificar si se proporcionaron nuevos valores y actualizar el pedido
                    if (nuevoEstado != null) {
                        pedido.estado = nuevoEstado
                    }

                    return@transaction true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@transaction false
                }
            }
        }

        fun obtenerPedidos(): List<PedidoDto> {
            return transaction {
                return@transaction Pedido.all().map { pedido ->
                    PedidoDto(
                        pedido.id_cliente.value,
                        pedido.estado.name
                    )
                }
            }
        }
    }

    var id_cliente by Pedidos.id_cliente
    var estado by Pedidos.estado
    var fecha_pedido by Pedidos.fecha_pedido
}
