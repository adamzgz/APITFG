import Categorias.entityId
import kotlinx.serialization.Serializable
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
    companion object : IntEntityClass<DetallePedido>(DetallesPedidos) {

        @Serializable
        data class DetallePedidoDto(
            val idPedido: Int,
            val idProducto: Int,
            val cantidad: Int
        )

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
                    this.id_pedido = pedido
                    this.id_producto = producto
                    this.cantidad = cantidad
                }
                println("Registro DetallePedido insertado con éxito")
            }
        }

        fun borrarDetallePedido(idDetalle: Int): Boolean {
            return transaction {
                val detalle = DetallePedido.findById(idDetalle)

                if (detalle == null) {
                    println("Detalle de pedido no encontrado con el id: $idDetalle")
                    return@transaction false
                }

                detalle.delete()
                println("Detalle de pedido borrado con éxito para el id: $idDetalle")
                return@transaction true
            }
        }

        fun actualizarCantidadDetallePedido(idDetalle: Int, nuevaCantidad: Int): Boolean {
            return transaction {
                val detalle = DetallePedido.findById(idDetalle)

                if (detalle == null) {
                    println("Detalle de pedido no encontrado con el id: $idDetalle")
                    return@transaction false
                }

                detalle.cantidad = nuevaCantidad
                println("Cantidad del detalle de pedido actualizada con éxito para el id: $idDetalle")
                return@transaction true
            }
        }
    }

    var id_pedido by Pedido referencedOn DetallesPedidos.id_pedido.entityId()
    var id_producto by Producto referencedOn DetallesPedidos.id_producto
    var cantidad by DetallesPedidos.cantidad
}
