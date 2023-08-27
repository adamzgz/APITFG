import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

object Pedidos : IntIdTable() {
    val id_cliente: Column<EntityID<Int>> = reference("id_cliente", Clientes)
    val estado: Column<EstadoPedido> = enumerationByName("estado", 50, EstadoPedido::class)
    val fecha_pedido: Column<LocalDate> = date("fecha_pedido")

    enum class EstadoPedido {
        PENDIENTE, ENVIADO, ENTREGADO, EN_PROCESO
    }
}

class Pedido(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Pedido>(Pedidos) {

        @Serializable
        data class PedidoDto(
            val idPedido: Int? = null,
            val idCliente: Int? = null,
            val estado: String,
            val fechaPedido: String? = null
        )

        fun crearPedido(idCliente: Int, estado: Pedidos.EstadoPedido): Pedido? {
            return transaction {
                val cliente = Cliente.findById(EntityID(idCliente, Clientes)) ?: return@transaction null
                val pedidoCreado = Pedido.new {
                    this.id_cliente = cliente.id
                    this.estado = estado
                    this.fecha_pedido = LocalDate.now()
                }
                return@transaction pedidoCreado
            }
        }

        fun borrarPedido(idPedido: Int): Boolean {
            return transaction {
                val pedido = Pedido.findById(idPedido) ?: return@transaction false
                pedido.delete()
                return@transaction true
            }
        }

        fun obtenerPedidosPorUsuario(idUsuario: Int): List<PedidoDto> {
            return transaction {
                val cliente = Cliente.findById(idUsuario) ?: return@transaction emptyList()
                return@transaction Pedido.find { Pedidos.id_cliente eq cliente.id }.map { pedido ->
                    PedidoDto(
                        pedido.id.value,
                        pedido.id_cliente.value,
                        pedido.estado.name,
                        pedido.fecha_pedido.toString()
                    )
                }
            }
        }

        fun pedidoEnProceso(idCliente: Int): PedidoDto? {
            return transaction {
                val cliente = Cliente.findById(idCliente) ?: return@transaction null
                val pedidoEncontrado = Pedido.find {
                    Pedidos.id_cliente eq cliente.id and (Pedidos.estado eq Pedidos.EstadoPedido.EN_PROCESO)
                }.singleOrNull()
                return@transaction pedidoEncontrado?.let { pedido ->
                    PedidoDto(
                        pedido.id.value,
                        pedido.id_cliente.value,
                        pedido.estado.name,
                        pedido.fecha_pedido.toString()
                    )
                }
            }
        }

        fun actualizarPedido(idPedido: Int, nuevoEstado: Pedidos.EstadoPedido): Boolean {
            return transaction {
                val pedido = Pedido.findById(idPedido) ?: return@transaction false
                pedido.estado = nuevoEstado
                return@transaction true
            }
        }

        fun obtenerPedidos(): List<PedidoDto> {
            return transaction {
                return@transaction Pedido.all().map { pedido ->
                    PedidoDto(
                        pedido.id.value,
                        pedido.id_cliente.value,
                        pedido.estado.name,
                        pedido.fecha_pedido.toString()
                    )
                }
            }
        }

        fun obtenerPedidoPorId(idPedido: Int): PedidoDto? {
            return transaction {
                val pedido = Pedido.findById(idPedido) ?: return@transaction null
                return@transaction PedidoDto(
                    pedido.id.value,
                    pedido.id_cliente.value,
                    pedido.estado.name,
                    pedido.fecha_pedido.toString()
                )
            }
        }

        fun obtenerPedidosPorCliente(idCliente: Int): List<PedidoDto> {
            return transaction {
                val cliente = Cliente.findById(idCliente) ?: return@transaction emptyList()
                return@transaction Pedido.find { Pedidos.id_cliente eq cliente.id }.map { pedido ->
                    println(pedido.fecha_pedido.toString())
                    PedidoDto(
                        pedido.id.value,
                        pedido.id_cliente.value,
                        pedido.estado.name,
                        pedido.fecha_pedido.toString()

                    )
                }
            }
        }
    }

    var id_cliente by Pedidos.id_cliente
    var estado by Pedidos.estado
    var fecha_pedido by Pedidos.fecha_pedido
}
