import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.`java-time`.date
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

object Pedidos : IntIdTable() {
    val id_cliente: Column<EntityID<Int>> = reference("id_cliente", Clientes)
    val estado: Column<EstadoPedido> = enumerationByName("estado", 50, EstadoPedido::class)
    val fecha_pedido: Column<LocalDate> = date("fecha_pedido")

    enum class EstadoPedido {
        PENDIENTE,
        ENVIADO,
        ENTREGADO
    }
}

class Pedido(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Pedido>(Pedidos)

    var id_cliente by Pedidos.id_cliente
    var estado by Pedidos.estado
    var fecha_pedido by Pedidos.fecha_pedido

    fun crearPedido(idCliente: Int, estado: Pedidos.EstadoPedido): Boolean {
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
}
