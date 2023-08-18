import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

object Mensajes : IntIdTable() {
    val id_chat = reference("id_chat", Chats)
    val id_usuario = reference("id_usuario", Usuarios)
    val mensaje = text("mensaje")
    val fecha_envio = datetime("fecha_envio")
}

class Mensaje(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Mensaje>(Mensajes) {
        @Serializable
        data class MensajeDto(
            val idChat: Int,
            val idUsuario: Int,
            val mensaje: String
        )

        fun crearMensaje(idChat: Int, idUsuario: Int, mensaje: String): Boolean {
            if (idChat <= 0 || idUsuario <= 0 || mensaje.isBlank()) {
                println("Datos ingresados no válidos.")
                return false
            }

            return transaction {
                try {
                    // Verificar si el ID de chat existe
                    val chat = Chat.findById(idChat)
                    if (chat == null) {
                        println("El chat con ID $idChat no existe.")
                        return@transaction false
                    }

                    // Verificar si el ID de usuario existe
                    val usuario = Usuario.findById(idUsuario)
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

        fun borrarMensaje(idMensaje: Int): Boolean {
            if (idMensaje <= 0) {
                println("ID de mensaje no válido.")
                return false
            }

            return transaction {
                try {
                    // Buscar el mensaje por su ID
                    val mensaje = Mensaje.findById(idMensaje)
                    if (mensaje == null) {
                        println("El mensaje con ID $idMensaje no existe.")
                        return@transaction false
                    }

                    // Eliminar el mensaje
                    mensaje.delete()

                    return@transaction true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@transaction false
                }
            }
        }

        fun actualizarMensaje(idMensaje: Int, nuevoMensaje: String): Boolean {
            if (idMensaje <= 0 || nuevoMensaje.isBlank()) {
                println("Datos ingresados no válidos.")
                return false
            }

            return transaction {
                try {
                    // Buscar el mensaje por su ID
                    val mensaje = Mensaje.findById(idMensaje)
                    if (mensaje == null) {
                        println("El mensaje con ID $idMensaje no existe.")
                        return@transaction false
                    }

                    // Actualizar el contenido del mensaje
                    mensaje.mensaje = nuevoMensaje

                    return@transaction true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@transaction false
                }
            }
        }

        fun obtenerMensajesDeChat(idChat: Int): List<MensajeDto> {
            if (idChat <= 0) {
                println("ID de chat no válido.")
                return emptyList()
            }

            return transaction {
                val chat = Chat.findById(idChat)
                if (chat != null) {
                    return@transaction Mensaje.find { Mensajes.id_chat eq chat.id }.map { mensaje ->
                        MensajeDto(
                            mensaje.id_chat.id.value,
                            mensaje.id_usuario.id.value,
                            mensaje.mensaje
                        )
                    }
                } else {
                    println("El chat con ID $idChat no existe.")
                    return@transaction emptyList()
                }
            }
        }
    }

    var id_chat by Chat referencedOn Mensajes.id_chat
    var id_usuario by Usuario referencedOn Mensajes.id_usuario
    var mensaje by Mensajes.mensaje
    var fecha_envio by Mensajes.fecha_envio
}
