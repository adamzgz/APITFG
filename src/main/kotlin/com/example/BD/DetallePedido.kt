import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object DetallesPedidos : IntIdTable() {
    val id_pedido = reference("id_pedido", Pedidos.id)
    val id_producto = reference("id_producto", Productos.id)
    val cantidad = integer("cantidad")
}

class DetallePedido(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DetallePedido>(DetallesPedidos)

    var pedido by DetallesPedidos.id_pedido
    var producto by DetallesPedidos.id_producto
    var cantidad by DetallesPedidos.cantidad
}
