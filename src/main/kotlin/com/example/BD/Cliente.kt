import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.transactions.transaction

object Clientes : IntIdTable() {
    val id_usuario = reference("id_usuario", Usuarios)
    val vip = bool("vip")
}

class Cliente(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Cliente>(Clientes) {
        @Serializable
        data class ClienteDto(
            val idUsuario: Int,
            val vip: Boolean
        )

        fun crearCliente(idUsuario: Int, vip: Boolean): Boolean {
            // Validar idUsuario
            if (idUsuario <= 0) {
                println("El ID del usuario no es válido.")
                return false
            }

            return transaction {
                try {
                    // Verificar si el ID de usuario existe
                    val usuario = Usuario.findById(idUsuario)
                    if (usuario == null) {
                        println("El usuario con ID $idUsuario no existe.")
                        return@transaction false
                    }

                    // Crear el nuevo cliente
                    Cliente.new {
                        this.id_usuario = usuario
                        this.vip = vip
                    }

                    return@transaction true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@transaction false
                }
            }
        }

        fun obtenerClientes(): List<ClienteDto> {
            return transaction {
                return@transaction Cliente.all().map { cliente ->
                    ClienteDto(cliente.id_usuario.id.value, cliente.vip)
                }
            }
        }

        fun obtenerCliente(id: Int): ClienteDto? {
            return transaction {
                val cliente = Cliente.findById(id)
                if (cliente != null) {
                    return@transaction ClienteDto(cliente.id_usuario.id.value, cliente.vip)
                } else {
                    return@transaction null
                }
            }
        }
        fun obtenerClientePorUsuario(idUsuario: Int): Cliente? {
            return transaction {
                Cliente.find { Clientes.id_usuario eq idUsuario }.singleOrNull()
            }
        }


        fun actualizarCliente(id: Int, idUsuario: Int? = null, vip: Boolean? = null): Boolean {
            // Validar id
            if (id <= 0) {
                println("El ID del cliente no es válido.")
                return false
            }

            return transaction {
                try {
                    // Buscar el cliente por su ID
                    val cliente = Cliente.findById(id)
                    if (cliente == null) {
                        println("El cliente con ID $id no existe.")
                        return@transaction false
                    }

                    // Verificar si se proporcionaron nuevos valores y actualizar el cliente
                    if (idUsuario != null) {
                        val usuario = Usuario.findById(idUsuario)
                        if (usuario == null) {
                            println("El usuario con ID $idUsuario no existe.")
                            return@transaction false
                        }
                        cliente.id_usuario = usuario
                    }
                    if (vip != null) {
                        cliente.vip = vip
                    }

                    return@transaction true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@transaction false
                }
            }
        }

        fun eliminarCliente(id: Int): Boolean {
            // Validar id
            if (id <= 0) {
                println("El ID del cliente no es válido.")
                return false
            }

            return transaction {
                try {
                    // Buscar el cliente por su ID
                    val cliente = Cliente.findById(id)
                    if (cliente == null) {
                        println("El cliente con ID $id no existe.")
                        return@transaction false
                    }

                    // Eliminar el cliente
                    cliente.delete()

                    return@transaction true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@transaction false
                }
            }
        }
    }

    var id_usuario by Usuario referencedOn Clientes.id_usuario
    var vip by Clientes.vip
}
