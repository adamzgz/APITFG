import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.transactions.transaction

object Categorias : IntIdTable() {
    val nombre = varchar("nombre", 50)
}

class Categoria(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Categoria>(Categorias) {
        @Serializable
        data class CategoriaDto(
            val nombre: String
        )

        fun crearCategoria(nombre: String): Boolean {
            // Verificar si el nombre está vacío o si es demasiado largo
            if (nombre.isEmpty() || nombre.length > 50) {
                println("El nombre de la categoría no puede estar vacío y debe tener 50 caracteres o menos.")
                return false
            }

            return transaction {
                try {
                    // Verificar si la categoría ya existe
                    val categoriaExistente = Categoria.find { Categorias.nombre eq nombre }.firstOrNull()
                    if (categoriaExistente != null) {
                        println("La categoría '$nombre' ya existe.")
                        return@transaction false
                    }

                    // Crear la nueva categoría
                    Categoria.new {
                        this.nombre = nombre
                    }

                    return@transaction true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@transaction false
                }
            }
        }

        fun obtenerCategorias(): List<CategoriaDto> {
            return transaction {
                return@transaction Categoria.all().map { categoria ->
                    CategoriaDto(categoria.nombre)
                }
            }
        }

        fun obtenerCategoria(id: Int): CategoriaDto? {
            return transaction {
                val categoria = Categoria.findById(id)
                if (categoria != null) {
                    return@transaction CategoriaDto(categoria.nombre)
                } else {
                    return@transaction null
                }
            }
        }

        fun actualizarCategoria(id: Int, nuevoNombre: String): Boolean {
            // Verificar si el nombre está vacío o si es demasiado largo
            if (nuevoNombre.isEmpty() || nuevoNombre.length > 50) {
                println("El nombre de la categoría no puede estar vacío y debe tener 50 caracteres o menos.")
                return false
            }

            return transaction {
                try {
                    // Buscar la categoría por su ID
                    val categoria = Categoria.findById(id)
                    if (categoria == null) {
                        println("La categoría con ID $id no existe.")
                        return@transaction false
                    }

                    // Verificar si la nueva categoría ya existe
                    val categoriaExistente = Categoria.find { Categorias.nombre eq nuevoNombre }.firstOrNull()
                    if (categoriaExistente != null && categoriaExistente != categoria) {
                        println("La categoría '$nuevoNombre' ya existe.")
                        return@transaction false
                    }

                    // Actualizar el nombre de la categoría
                    categoria.nombre = nuevoNombre

                    return@transaction true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@transaction false
                }
            }
        }

        fun eliminarCategoria(id: Int): Boolean {
            return transaction {
                try {
                    // Buscar la categoría por su ID
                    val categoria = Categoria.findById(id)
                    if (categoria == null) {
                        println("La categoría con ID $id no existe.")
                        return@transaction false
                    }

                    // Eliminar la categoría
                    categoria.delete()

                    return@transaction true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@transaction false
                }
            }
        }
    }

    var nombre by Categorias.nombre
}
