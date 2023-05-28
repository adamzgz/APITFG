import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal

class Producto(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Producto>(Productos)

    var nombre by Productos.nombre
    var descripcion by Productos.descripcion
    var precio by Productos.precio
    var stock by Productos.stock
    var categoria by Categoria referencedOn Productos.id_categoria
    var imagen by Productos.imagen

    fun crearProducto(
        nombre: String,
        descripcion: String,
        precio: BigDecimal,
        stock: Int,
        idCategoria: Int,
        imagen: String
    ): Boolean {
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
                    this.precio = precio
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
}

object Productos : IntIdTable() {
    val nombre = varchar("nombre", 50)
    val descripcion = text("descripcion")
    val precio = decimal("precio", 10, 2)
    val stock = integer("stock")
    val id_categoria = reference("id_categoria", Categorias)
    val imagen = varchar("imagen", 100) // Nombre de la imagen (archivo)
}
