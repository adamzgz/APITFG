import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDateTime

object Valoraciones : IntIdTable() {
    val id_cliente = reference("id_cliente", Clientes)
    val id_producto = reference("id_producto", Productos)
    val puntuacion = integer("puntuacion")
    val comentario = text("comentario").nullable()
    val fecha = datetime("fecha")
}

class Valoracion(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Valoracion>(Valoraciones) {
        @Serializable
        data class ValoracionDto(
            val idCliente: Int,
            val idProducto: Int,
            val puntuacion: Int,
            val comentario: String?
        )

        fun crearValoracion(
            idCliente: Int,
            idProducto: Int,
            puntuacion: Int,
            comentario: String?
        ): Boolean {
            if (idCliente <= 0 || idProducto <= 0 || puntuacion <= 0) {
                println("Datos ingresados no válidos.")
                return false
            }

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
                        this.fecha = LocalDateTime.now()
                    }

                    return@transaction true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@transaction false
                }
            }
        }

        fun borrarValoracion(idValoracion: Int): Boolean {
            if (idValoracion <= 0) {
                println("ID de valoración no válido.")
                return false
            }

            return transaction {
                try {
                    // Buscar la valoración por su ID
                    val valoracion = Valoracion.findById(idValoracion)
                    if (valoracion == null) {
                        println("La valoración con ID $idValoracion no existe.")
                        return@transaction false
                    }

                    // Eliminar la valoración
                    valoracion.delete()

                    return@transaction true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@transaction false
                }
            }
        }

        fun actualizarValoracion(
            idValoracion: Int,
            nuevaPuntuacion: Int? = null,
            nuevoComentario: String? = null
        ): Boolean {
            if (idValoracion <= 0 || (nuevaPuntuacion != null && nuevaPuntuacion <= 0)) {
                println("Datos ingresados no válidos.")
                return false
            }

            return transaction {
                try {
                    // Buscar la valoración por su ID
                    val valoracion = Valoracion.findById(idValoracion)
                    if (valoracion == null) {
                        println("La valoración con ID $idValoracion no existe.")
                        return@transaction false
                    }

                    // Verificar si se proporcionaron nuevos valores y actualizar la valoración
                    if (nuevaPuntuacion != null) {
                        valoracion.puntuacion = nuevaPuntuacion
                    }
                    if (nuevoComentario != null && !nuevoComentario.isBlank()) {
                        valoracion.comentario = nuevoComentario
                    }

                    return@transaction true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@transaction false
                }
            }
        }

        fun obtenerPuntuacionMediaPorProducto(idProducto: Int): BigDecimal? {
            if (idProducto <= 0) {
                println("ID de producto no válido.")
                return null
            }

            return transaction {
                val producto = Producto.findById(idProducto)
                if (producto != null) {
                    val puntuaciones = Valoracion.find { Valoraciones.id_producto eq producto.id }
                        .map { it.puntuacion }

                    if (puntuaciones.isNotEmpty()) {
                        val sumaPuntuaciones = puntuaciones.sum()
                        val media = sumaPuntuaciones.toDouble() / puntuaciones.size.toDouble()
                        return@transaction BigDecimal(media).setScale(2, BigDecimal.ROUND_HALF_UP)
                    }
                }
                return@transaction null
            }
        }
        fun obtenerValoracionesPorProducto(idProducto: Int): List<ValoracionDto> {
            if (idProducto <= 0) {
                println("ID de producto no válido.")
                return emptyList()
            }

            return transaction {
                try {
                    // Buscar valoraciones por id_producto
                    val valoraciones = Valoracion.find { Valoraciones.id_producto eq idProducto }
                    valoraciones.map {
                        ValoracionDto(
                            idCliente = it.id_cliente.id.value,
                            idProducto = it.id_producto.id.value,
                            puntuacion = it.puntuacion,
                            comentario = it.comentario
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList<ValoracionDto>()
                }
            }
        }

    }

    var id_cliente by Cliente referencedOn Valoraciones.id_cliente
    var id_producto by Producto referencedOn Valoraciones.id_producto
    var puntuacion by Valoraciones.puntuacion
    var comentario by Valoraciones.comentario
    var fecha by Valoraciones.fecha
}
