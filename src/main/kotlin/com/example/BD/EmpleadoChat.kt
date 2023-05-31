import kotlinx.serialization.Serializable
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
    companion object : IntEntityClass<EmpleadoChat>(EmpleadosChat) {
        @Serializable
        data class EmpleadoChatDto(
            val idEmpleado: Int,
            val idChat: Int
        )

        fun insertEmpleadoChat(idEmpleado: Int, idChat: Int): Boolean {
            if (idEmpleado <= 0 || idChat <= 0) {
                println("Datos ingresados no válidos.")
                return false
            }

            return transaction {
                val empleado = Empleado.findById(idEmpleado)
                val chat = Chat.findById(idChat)

                if (empleado == null) {
                    println("Empleado no encontrado con el id: $idEmpleado")
                    return@transaction false
                }

                if (chat == null) {
                    println("Chat no encontrado con el id: $idChat")
                    return@transaction false
                }

                EmpleadoChat.new {
                    this.id_empleado = empleado
                    this.id_chat = chat
                }
                println("Registro EmpleadoChat insertado con éxito")
                return@transaction true
            }
        }

        fun borrarEmpleadoChat(idEmpleadoChat: Int): Boolean {
            if (idEmpleadoChat <= 0) {
                println("ID de EmpleadoChat no válido.")
                return false
            }

            return transaction {
                val empleadoChat = EmpleadoChat.findById(idEmpleadoChat)

                if (empleadoChat == null) {
                    println("EmpleadoChat no encontrado con el id: $idEmpleadoChat")
                    return@transaction false
                }

                empleadoChat.delete()
                println("EmpleadoChat borrado con éxito para el id: $idEmpleadoChat")
                return@transaction true
            }
        }

        fun actualizarEmpleadoChat(idEmpleadoChat: Int, nuevoIdEmpleado: Int, nuevoIdChat: Int): Boolean {
            if (idEmpleadoChat <= 0 || nuevoIdEmpleado <= 0 || nuevoIdChat <= 0) {
                println("Datos ingresados no válidos.")
                return false
            }

            return transaction {
                val empleadoChat = EmpleadoChat.findById(idEmpleadoChat)

                if (empleadoChat == null) {
                    println("EmpleadoChat no encontrado con el id: $idEmpleadoChat")
                    return@transaction false
                }

                val nuevoEmpleado = Empleado.findById(nuevoIdEmpleado)
                val nuevoChat = Chat.findById(nuevoIdChat)

                if (nuevoEmpleado == null) {
                    println("Nuevo empleado no encontrado con el id: $nuevoIdEmpleado")
                    return@transaction false
                }

                if (nuevoChat == null) {
                    println("Nuevo chat no encontrado con el id: $nuevoIdChat")
                    return@transaction false
                }

                empleadoChat.id_empleado = nuevoEmpleado
                empleadoChat.id_chat = nuevoChat

                println("EmpleadoChat actualizado con éxito para el id: $idEmpleadoChat")
                return@transaction true
            }
        }
    }

    var id_empleado by Empleado referencedOn EmpleadosChat.id_empleado
    var id_chat by Chat referencedOn EmpleadosChat.id_chat
}
