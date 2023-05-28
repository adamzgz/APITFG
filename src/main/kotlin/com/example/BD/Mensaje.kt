import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.`java-time`.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

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

    fun crearMensaje(idChat: Int, idUsuario: Int, mensaje: String): Boolean {
        return transaction {
            try {
                // Verificar si el ID de chat existe
                val chat = Chat.findById(EntityID(idChat, Chats))
                if (chat == null) {
                    println("El chat con ID $idChat no existe.")
                    return@transaction false
                }

                // Verificar si el ID de usuario existe
                val usuario = Usuario.findById(EntityID(idUsuario, Usuarios))
                if (usuario == null) {
                    println("El usuario con ID $idUsuario no existe.")
                    return@transaction false
                }

                // Crear el nuevo mensaje
                Mensaje.new {
                    this.id_chat = chat
                    this.id_usuario = usuario
                    this.mensaje = mensaje
                    this.fecha_envio = LocalDateTime.now()
                }

                return@transaction true
            } catch (e: Exception) {
                e.printStackTrace()
                return@transaction false
            }
        }
    }
}
