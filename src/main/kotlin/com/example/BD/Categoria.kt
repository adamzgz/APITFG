import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.transactions.transaction

object Categorias : IntIdTable() {
    val nombre = varchar("nombre", 50)
}

class Categoria(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Categoria>(Categorias)

    var nombre by Categorias.nombre

    fun crearCategoria(nombre: String): Boolean {
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
}
