import EmpleadosChat.entityId
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.transactions.transaction

object DetallesPedidos : IntIdTable() {
    val id_pedido = reference("id_pedido", Pedidos)
    val id_producto = reference("id_producto", Productos)
    val cantidad = integer("cantidad")
}

class DetallePedido(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DetallePedido>(DetallesPedidos)

    var id_pedido by Pedido referencedOn DetallesPedidos.id_pedido.entityId()
    var id_producto by Producto referencedOn DetallesPedidos.id_producto
    var cantidad by DetallesPedidos.cantidad

    fun insertDetallePedido(idPedido: Int, idProducto: Int, cantidad: Int) {
        transaction {
            val pedido = Pedido.findById(idPedido)
            val producto = Producto.findById(idProducto)

            if (pedido == null) {
                println("Pedido no encontrado con el id: $idPedido")
                return@transaction
            }

            if (producto == null) {
                println("Producto no encontrado con el id: $idProducto")
                return@transaction
            }

            DetallePedido.new {
                id_pedido = pedido
                id_producto = producto
                this.cantidad = cantidad
            }
            println("Registro DetallePedido insertado con éxito")
        }
    }
}
