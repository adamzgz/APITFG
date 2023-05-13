import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Clientes : IntIdTable() {
    val id_usuario = reference("id_usuario", Usuarios)
    val vip = bool("vip")
}

class Cliente(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Cliente>(Clientes)

    var id_usuario by Usuario referencedOn Clientes.id_usuario
    var vip by Clientes.vip
}
