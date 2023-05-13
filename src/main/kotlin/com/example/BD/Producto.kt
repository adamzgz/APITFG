import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

class Producto(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Producto>(Productos)

    var nombre by Productos.nombre
    var descripcion by Productos.descripcion
    var precio by Productos.precio
    var stock by Productos.stock
    var categoria by Categoria referencedOn Productos.id_categoria
}

object Productos : IntIdTable() {
    val nombre = varchar("nombre", 50)
    val descripcion = text("descripcion")
    val precio = decimal("precio", 10, 2)
    val stock = integer("stock")
    val id_categoria = reference("id_categoria", Categorias)
}
