import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.and
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
            val id: Int?,
            val idPedido: Int,
            val idProducto: Int,
            val cantidad: Int
        )

        fun insertDetallePedido(idPedido: Int, idProducto: Int, cantidad: Int): Boolean {
            if (idPedido <= 0 || idProducto <= 0 || cantidad <= 0) {
                println("Datos ingresados no válidos.")
                return false
            }

            return transaction {
                val pedido = Pedido.findById(idPedido)
                val producto = Producto.findById(idProducto)

                if (pedido == null) {
                    println("Pedido no encontrado con el id: $idPedido")
                    return@transaction false
                }

                if (producto == null) {
                    println("Producto no encontrado con el id: $idProducto")
                    return@transaction false
                }

                // Comprobar que el estado del pedido está EN_PROCESO antes de agregar el detalle
                if (pedido.estado != Pedidos.EstadoPedido.EN_PROCESO) {
                    println("El pedido con ID $idPedido no está en proceso, por lo que no se pueden agregar más detalles.")
                    return@transaction false
                }

                // Comprobar si ya existe un DetallePedido con ese idPedido y idProducto
                val detalleExistente = DetallePedido.find {
                    DetallesPedidos.id_pedido eq idPedido and (DetallesPedidos.id_producto eq idProducto)
                }.singleOrNull()

                if (detalleExistente != null) {
                    // Si ya existe, actualizamos la cantidad
                    detalleExistente.cantidad += cantidad
                    println("Cantidad actualizada en el DetallePedido existente")
                } else {
                    // Si no existe, creamos un nuevo detalle
                    DetallePedido.new {
                        this.id_pedido = pedido
                        this.id_producto = producto
                        this.cantidad = cantidad
                    }
                    println("Registro DetallePedido insertado con éxito")
                }

                return@transaction true
            }
        }

        fun borrarDetallePedido(idDetalle: Int): Boolean {
            if (idDetalle <= 0) {
                println("El ID del detalle de pedido no es válido.")
                return false
            }

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
            if (idDetalle <= 0 || nuevaCantidad <= 0) {
                println("Datos ingresados no válidos.")
                return false
            }

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

    var id_pedido by Pedido referencedOn DetallesPedidos.id_pedido
    var id_producto by Producto referencedOn DetallesPedidos.id_producto
    var cantidad by DetallesPedidos.cantidad
}
