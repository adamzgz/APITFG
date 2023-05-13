import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.`java-time`.datetime

object Mensajes : IntIdTable() {
    val id_chat = reference("id_chat", Chats)
    val id_usuario = reference("id_usuario", Usuarios)
    val mensaje = text("mensaje")
    val fecha_envio = datetime("fecha_envio")
}

class Mensaje(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Mensaje>(Mensajes)

    var id_chat by Chat referencedOn Mensajes.id_chat
    var id_usuario by Usuario referencedOn Mensajes.id_usuario
    var mensaje by Mensajes.mensaje
    var fecha_envio by Mensajes.fecha_envio
}
