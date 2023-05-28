import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.transactions.transaction

object EmpleadosChat : IntIdTable() {
    val id_empleado = reference("id_empleado", Empleados)
    val id_chat = reference("id_chat", Chats)
}

class EmpleadoChat(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<EmpleadoChat>(EmpleadosChat)

    var id_empleado by Empleado referencedOn EmpleadosChat.id_empleado
    var id_chat by Chat referencedOn EmpleadosChat.id_chat


    fun insertEmpleadoChat(idEmpleado: Int, idChat: Int) {
        transaction {
            val empleado = Empleado.findById(idEmpleado)
            val chat = Chat.findById(idChat)

            if (empleado == null) {
                println("Empleado no encontrado con el id: $idEmpleado")
                return@transaction
            }

            if (chat == null) {
                println("Chat no encontrado con el id: $idChat")
                return@transaction
            }

            EmpleadoChat.new {
                id_empleado = empleado
                id_chat = chat
            }
            println("Registro EmpleadoChat insertado con éxito")
        }
    }
}
