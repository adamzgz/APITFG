import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

class Producto(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Producto>(Productos) {

        @Serializable
        data class ProductoDto(
            val id: Int,
            val nombre: String,
            val descripcion: String,
            val precio: Double,
            val idCategoria: Int,
            val imagen: String
        )

        fun crearProducto(
            nombre: String,
            descripcion: String,
            precio: Double,
            idCategoria: Int,
            imagen: String
        ): Boolean {  // Devuelve un valor Boolean
            if (nombre.isBlank() || descripcion.isBlank() || precio <= 0 || idCategoria <= 0 || imagen.isBlank()) {
                println("Datos ingresados no válidos.")
                return false  // Devuelve false si la validación falla
            }

            return transaction {
                try {
                    // Verificar si el ID de categoría existe
                    val categoria = Categoria.findById(idCategoria)
                    if (categoria == null) {
                        println("La categoría con ID $idCategoria no existe.")
                        return@transaction false  // Devuelve false si la categoría no existe
                    }

                    // Crear el nuevo producto
                    val nuevoProducto = Producto.new {
                        this.nombre = nombre
                        this.descripcion = descripcion
                        this.precio = precio.toBigDecimal()
                        this.categoria = categoria
                        this.imagen = imagen
                    }

                    return@transaction true  // Devuelve true si todo va bien
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@transaction false  // Devuelve false si hay un error
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
            nuevoIdCategoria: Int? = null,
            nuevaImagen: String? = null
        ): Boolean {
            if (idProducto <= 0 || (nuevoPrecio != null && nuevoPrecio <= 0) || (nuevoIdCategoria != null && nuevoIdCategoria <= 0)) {
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
                Producto.all().map { producto ->
                    ProductoDto(
                        producto.id.value,  // Añade el id del producto aquí
                        producto.nombre,
                        producto.descripcion,
                        producto.precio.toDouble(),
                        producto.categoria.id.value,
                        producto.imagen
                    )
                }
            }
        }
        fun obtenerProductoPorId(idProducto: Int): ProductoDto? {
            return transaction {
                val producto = Producto.findById(idProducto)
                if (producto != null) {
                    ProductoDto(
                        producto.id.value,
                        producto.nombre,
                        producto.descripcion,
                        producto.precio.toDouble(),
                        producto.categoria.id.value,
                        producto.imagen
                    )
                } else {
                    null
                }
            }
        }
        fun actualizarNombreImagen(idProducto: Int, nuevoNombreImagen: String): Boolean {
            if (idProducto <= 0 || nuevoNombreImagen.isBlank()) {
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

                    // Actualizar el nombre de la imagen
                    producto.imagen = nuevoNombreImagen

                    return@transaction true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@transaction false
                }
            }
        }


    }

    var nombre by Productos.nombre
    var descripcion by Productos.descripcion
    var precio by Productos.precio
    var categoria by Categoria referencedOn Productos.id_categoria
    var imagen by Productos.imagen
}

object Productos : IntIdTable() {
    val nombre = varchar("nombre", 50)
    val descripcion = text("descripcion")
    val precio = decimal("precio", 10, 2)
    val id_categoria = reference("id_categoria", Categorias)
    val imagen = varchar("imagen", 100) // Nombre de la imagen (archivo)
}
