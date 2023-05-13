import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object EmpleadosChat : IntIdTable() {
    val id_empleado = reference("id_empleado", Empleados)
    val id_chat = reference("id_chat", Chats)
}

class EmpleadoChat(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<EmpleadoChat>(EmpleadosChat)

    var id_empleado by EmpleadosChat.id_empleado
    var id_chat by Chat referencedOn EmpleadosChat.id_chat
}
