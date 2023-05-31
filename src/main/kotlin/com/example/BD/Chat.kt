import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.transactions.transaction

object Chats : IntIdTable() {
    val id_pedido = reference("id_pedido", Pedidos)
    val estado = enumerationByName("estado", 20, Estado::class).default(Estado.ABIERTO)

    enum class Estado {
        ABIERTO, CERRADO, EN_PROGRESO
    }
}

class Chat(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Chat>(Chats) {
        @Serializable
        data class ChatDto(
            val idPedido: Int,
            val estado: Chats.Estado
        )

        fun crearChat(idPedido: Int): Chat? {
            // Validar idPedido
            if (idPedido <= 0) {
                println("El ID del pedido no es válido.")
                return null
            }

            return transaction {
                val pedido = Pedido.findById(idPedido)

                if (pedido == null) {
                    println("Pedido no encontrado con el id: $idPedido")
                    return@transaction null
                }

                Chat.new {
                    this.id_pedido = pedido
                    this.estado = Chats.Estado.ABIERTO
                }.also {
                    println("Chat creado con éxito para el pedido id: $idPedido")
                }
            }
        }

        fun borrarChat(idChat: Int): Boolean {
            return transaction {
                val chat = Chat.findById(idChat)

                if (chat == null) {
                    println("Chat no encontrado con el id: $idChat")
                    return@transaction false
                }

                chat.delete()
                println("Chat borrado con éxito para el id: $idChat")
                return@transaction true
            }
        }

        fun actualizarEstadoChat(idChat: Int, nuevoEstado: Chats.Estado): Boolean {
            // Validar idChat
            if (idChat <= 0) {
                println("El ID del chat no es válido.")
                return false
            }

            return transaction {
                val chat = Chat.findById(idChat)

                if (chat == null) {
                    println("Chat no encontrado con el id: $idChat")
                    return@transaction false
                }

                chat.estado = nuevoEstado
                println("Estado del chat actualizado con éxito para el id: $idChat")
                return@transaction true
            }
        }

        fun obtenerChats(): List<ChatDto> {
            return transaction {
                return@transaction Chat.all().map { chat ->
                    ChatDto(chat.id_pedido.id.value, chat.estado)
                }
            }
        }

        fun obtenerChat(id: Int): ChatDto? {
            return transaction {
                val chat = Chat.findById(id)
                if (chat != null) {
                    return@transaction ChatDto(chat.id_pedido.id.value, chat.estado)
                } else {
                    return@transaction null
                }
            }
        }
    }

    var id_pedido by Pedido referencedOn Chats.id_pedido
    var estado by Chats.estado
}
