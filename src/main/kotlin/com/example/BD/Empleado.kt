import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.transactions.transaction

object Empleados : IntIdTable() {
    val id_usuario: Column<EntityID<Int>> = reference("id_usuario", Usuarios)
    val rol: Column<RolEmpleado> = enumerationByName("rol", 50, RolEmpleado::class)

    init {
        uniqueIndex("unique_id_usuario", id_usuario)
    }

    enum class RolEmpleado {
        ADMINISTRADOR,
        LOGISTICA,
        SOPORTE
    }
}

class Empleado(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Empleado>(Empleados) {
        @Serializable
        data class EmpleadoDto(
            val idUsuario: Int,
            val rol: String
        )

        fun crearEmpleado(idUsuario: Int, rol: String): Boolean {
            return transaction {
                try {
                    // Verificar si el ID de usuario existe
                    val usuario = Usuario.findById(idUsuario)
                    if (usuario == null) {
                        println("El usuario con ID $idUsuario no existe.")
                        return@transaction false
                    }

                    // Crear el nuevo empleado
                    Empleado.new {
                        this.id_usuario = usuario
                        this.rol = Empleados.RolEmpleado.valueOf(rol)
                    }

                    return@transaction true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@transaction false
                }
            }
        }

        fun obtenerEmpleados(): List<EmpleadoDto> {
            return transaction {
                return@transaction Empleado.all().map { empleado ->
                    EmpleadoDto(empleado.id_usuario.id.value, empleado.rol.name)
                }
            }
        }

        fun obtenerEmpleado(id: Int): EmpleadoDto? {
            return transaction {
                val empleado = Empleado.findById(id)
                if (empleado != null) {
                    return@transaction EmpleadoDto(empleado.id_usuario.id.value, empleado.rol.name)
                } else {
                    return@transaction null
                }
            }
        }

        fun actualizarEmpleado(id: Int, idUsuario: Int? = null, rol: String? = null): Boolean {
            return transaction {
                try {
                    // Buscar el empleado por su ID
                    val empleado = Empleado.findById(id)
                    if (empleado == null) {
                        println("El empleado con ID $id no existe.")
                        return@transaction false
                    }

                    // Verificar si se proporcionaron nuevos valores y actualizar el empleado
                    if (idUsuario != null) {
                        val usuario = Usuario.findById(idUsuario)
                        if (usuario == null) {
                            println("El usuario con ID $idUsuario no existe.")
                            return@transaction false
                        }
                        empleado.id_usuario = usuario
                    }
                    if (rol != null) {
                        empleado.rol = Empleados.RolEmpleado.valueOf(rol)
                    }

                    return@transaction true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@transaction false
                }
            }
        }

        fun eliminarEmpleado(id: Int): Boolean {
            return transaction {
                try {
                    // Buscar el empleado por su ID
                    val empleado = Empleado.findById(id)
                    if (empleado == null) {
                        println("El empleado con ID $id no existe.")
                        return@transaction false
                    }

                    // Eliminar el empleado
                    empleado.delete()

                    return@transaction true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@transaction false
                }
            }
        }
    }

    var id_usuario by Usuario referencedOn Empleados.id_usuario
    var rol by Empleados.rol
}
