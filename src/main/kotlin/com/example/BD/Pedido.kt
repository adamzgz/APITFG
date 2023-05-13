
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.`java-time`.date
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
}
