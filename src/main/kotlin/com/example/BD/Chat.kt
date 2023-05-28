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
    companion object : IntEntityClass<Chat>(Chats)

    var id_pedido by Pedido referencedOn Chats.id_pedido
    var estado by Chats.estado

    fun crearChat(idPedido: Int): Chat? {
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
}
