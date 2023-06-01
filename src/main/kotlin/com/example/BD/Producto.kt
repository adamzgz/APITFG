import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.transactions.transaction
class Producto(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Producto>(Productos) {

        @Serializable
        data class ProductoDto(
            val nombre: String,
            val descripcion: String,
            val precio: Double,
            val stock: Int,
            val idCategoria: Int,
            val imagen: String
        )

        fun crearProducto(
            nombre: String,
            descripcion: String,
            precio: Double,
            stock: Int,
            idCategoria: Int,
            imagen: String
        ): Boolean {
            if (nombre.isBlank() || descripcion.isBlank() || precio <= 0 || stock <= 0 || idCategoria <= 0 || imagen.isBlank()) {
                println("Datos ingresados no válidos.")
                return false
            }

            return transaction {
                try {
                    // Verificar si el ID de categoría existe
                    val categoria = Categoria.findById(idCategoria)
                    if (categoria == null) {
                        println("La categoría con ID $idCategoria no existe.")
                        return@transaction false
                    }

                    // Crear el nuevo producto
                    Producto.new {
                        this.nombre = nombre
                        this.descripcion = descripcion
                        this.precio = precio.toBigDecimal()
                        this.stock = stock
                        this.categoria = categoria
                        this.imagen = imagen
                    }

                    return@transaction true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@transaction false
                }
            }
        }

        fun borrarProducto(idProducto: Int): Boolean {
            if (idProducto <= 0) {
                println("ID de producto no válido.")
                return false
            }

            return transaction {
                try {
                    // Buscar el producto por su ID
                    val producto = Producto.findById(idProducto)
                    if (producto == null) {
                        println("El producto con ID $idProducto no existe.")
                        return@transaction false
                    }

                    // Eliminar el producto
                    producto.delete()

                    return@transaction true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@transaction false
                }
            }
        }

        fun actualizarProducto(
            idProducto: Int,
            nuevoNombre: String? = null,
            nuevaDescripcion: String? = null,
            nuevoPrecio: Double? = null,
            nuevoStock: Int? = null,
            nuevoIdCategoria: Int? = null,
            nuevaImagen: String? = null
        ): Boolean {
            if (idProducto <= 0 || (nuevoPrecio != null && nuevoPrecio <= 0) || (nuevoStock != null && nuevoStock <= 0) || (nuevoIdCategoria != null && nuevoIdCategoria <= 0)) {
                println("Datos ingresados no válidos.")
                return false
            }

            return transaction {
                try {
                    // Buscar el producto por su ID
                    val producto = Producto.findById(idProducto)
                    if (producto == null) {
                        println("El producto con ID $idProducto no existe.")
                        return@transaction false
                    }

                    // Verificar si se proporcionaron nuevos valores y actualizar el producto
                    if (nuevoNombre != null && !nuevoNombre.isBlank()) {
                        producto.nombre = nuevoNombre
                    }
                    if (nuevaDescripcion != null && !nuevaDescripcion.isBlank()) {
                        producto.descripcion = nuevaDescripcion
                    }
                    if (nuevoPrecio != null) {
                        producto.precio = nuevoPrecio.toBigDecimal()
                    }
                    if (nuevoStock != null) {
                        producto.stock = nuevoStock
                    }
                    if (nuevoIdCategoria != null) {
                        val nuevaCategoria = Categoria.findById(nuevoIdCategoria)
                        if (nuevaCategoria == null) {
                            println("La categoría con ID $nuevoIdCategoria no existe.")
                            return@transaction false
                        }
                        producto.categoria = nuevaCategoria
                    }
                    if (nuevaImagen != null && !nuevaImagen.isBlank()) {
                        producto.imagen = nuevaImagen
                    }

                    return@transaction true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@transaction false
                }
            }
        }

        fun obtenerProductos(): List<ProductoDto> {
            return transaction {
                return@transaction Producto.all().map { producto ->
                    ProductoDto(
                        producto.nombre,
                        producto.descripcion,
                        producto.precio.toDouble(),
                        producto.stock,
                        producto.categoria.id.value,
                        producto.imagen
                    )
                }
            }
        }
    }

    var nombre by Productos.nombre
    var descripcion by Productos.descripcion
    var precio by Productos.precio
    var stock by Productos.stock
    var categoria by Categoria referencedOn Productos.id_categoria
    var imagen by Productos.imagen
}

object Productos : IntIdTable() {
    val nombre = varchar("nombre", 50)
    val descripcion = text("descripcion")
    val precio = decimal("precio", 10, 2)
    val stock = integer("stock")
    val id_categoria = reference("id_categoria", Categorias)
    val imagen = varchar("imagen", 100) // Nombre de la imagen (archivo)
}
