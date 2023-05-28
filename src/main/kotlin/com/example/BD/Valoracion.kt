import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.`java-time`.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

object Valoraciones : IntIdTable() {
    val id_cliente = reference("id_cliente", Clientes)
    val id_producto = reference("id_producto", Productos)
    val puntuacion = integer("puntuacion")
    val comentario = text("comentario").nullable()
    val fecha_valoracion = datetime("fecha_valoracion")
}

class Valoracion(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Valoracion>(Valoraciones)

    var id_cliente by Cliente referencedOn Valoraciones.id_cliente
    var id_producto by Producto referencedOn Valoraciones.id_producto
    var puntuacion by Valoraciones.puntuacion
    var comentario by Valoraciones.comentario
    var fecha_valoracion by Valoraciones.fecha_valoracion

    fun crearValoracion(
        idCliente: Int,
        idProducto: Int,
        puntuacion: Int,
        comentario: String?
    ): Boolean {
        return transaction {
            try {
                // Verificar si el ID de cliente existe
                val cliente = Cliente.findById(idCliente)
                if (cliente == null) {
                    println("El cliente con ID $idCliente no existe.")
                    return@transaction false
                }

                // Verificar si el ID de producto existe
                val producto = Producto.findById(idProducto)
                if (producto == null) {
                    println("El producto con ID $idProducto no existe.")
                    return@transaction false
                }

                // Crear la nueva valoración
                Valoracion.new {
                    this.id_cliente = cliente
                    this.id_producto = producto
                    this.puntuacion = puntuacion
                    this.comentario = comentario
                    this.fecha_valoracion = LocalDateTime.now()
                }

                return@transaction true
            } catch (e: Exception) {
                e.printStackTrace()
                return@transaction false
            }
        }
    }
}
