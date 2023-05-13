import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

object Chats : IntIdTable() {
    val id_pedido = reference("id_pedido", Pedidos)
    val estado: Column<Chat.Estado> = enumerationByName("estado", 20, Chat.Estado::class)
}

class Chat(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Chat>(Chats)

    var pedido by Chats.id_pedido
    var estado by Chats.estado

    enum class Estado {
        ABIERTO, CERRADO, EN_PROGRESO
    }
}
