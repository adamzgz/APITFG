
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Categorias : IntIdTable() {
    val nombre = varchar("nombre", 50)
}

class Categoria(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Categoria>(Categorias)

    var nombre by Categorias.nombre
}
