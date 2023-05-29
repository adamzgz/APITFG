import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

object Usuarios : IntIdTable() {
    val nombre = varchar("nombre", 50)
    val direccion = varchar("direccion", 100)
    val telefono = varchar("telefono", 20)
    val email = varchar("email", 100)
    val contraseña = varchar("contraseña", 255)
}

class Usuario(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Usuario>(Usuarios) {

        @Serializable
        data class UsuarioDto(
            val nombre: String,
            val direccion: String,
            val telefono: String,
            val email: String,
            val contraseña: String
        )

        @Serializable
        data class EmpleadoDto(
            val nombre: String,
            val direccion: String,
            val telefono: String,
            val email: String,
            val contraseña: String,
            val rol: String
        )

        fun registrar(usuarioDto: UsuarioDto): Boolean {
            return transaction {
                try {
                    validarEmailUnico(usuarioDto.email)
                    validarTelefonoUnico(usuarioDto.telefono)
                    validarContraseña(usuarioDto.contraseña)

                    val contraseñaCifrada = cifrarContraseña(usuarioDto.contraseña)

                    val nuevoUsuario = Usuario.new {
                        nombre = usuarioDto.nombre
                        direccion = usuarioDto.direccion
                        telefono = usuarioDto.telefono
                        email = usuarioDto.email
                        contraseña = contraseñaCifrada
                    }

                    // Crear instancia en la tabla "Clientes"
                    Cliente.new {
                        id_usuario = nuevoUsuario
                        vip = false
                    }

                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
        }


        fun registrarEmpleado(empleadoDto: EmpleadoDto): Boolean {
            return transaction {
                try {
                    validarEmailUnico(empleadoDto.email)
                    validarTelefonoUnico(empleadoDto.telefono)
                    validarContraseña(empleadoDto.contraseña)

                    val contraseñaCifrada = cifrarContraseña(empleadoDto.contraseña)

                    val nuevoUsuario = Usuario.new {
                        nombre = empleadoDto.nombre
                        direccion = empleadoDto.direccion
                        telefono = empleadoDto.telefono
                        email = empleadoDto.email
                        contraseña = contraseñaCifrada
                    }

                    // Convertir el string de rol a una instancia de RolEmpleado
                    val rolEmpleado = Empleados.RolEmpleado.valueOf(empleadoDto.rol.toUpperCase())

                    // Crear instancia en la tabla "Empleados"
                    Empleado.new {
                        id_usuario = nuevoUsuario
                        rol = rolEmpleado
                    }

                    return@transaction true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@transaction false
                }
            }
        }

        private fun cifrarContraseña(contraseña: String): String {
            return BCrypt.hashpw(contraseña, BCrypt.gensalt())
        }

        private fun validarEmailUnico(email: String) {
            if (transaction { Usuario.find { Usuarios.email eq email }.count() > 0 }) {
                throw IllegalArgumentException("El email ya está registrado")
            }
        }

        private fun validarTelefonoUnico(telefono: String) {
            if (transaction { Usuario.find { Usuarios.telefono eq telefono }.count() > 0 }) {
                throw IllegalArgumentException("El teléfono ya está registrado")
            }
        }

        private fun validarContraseña(contraseña: String) {
            val regex = Regex("^(?=.*[A-Z])(?=.*[0-9]).+\$")
            if (!contraseña.matches(regex)) {
                throw IllegalArgumentException("La contraseña debe contener al menos una letra mayúscula y un número")
            }
        }

    }

    var nombre by Usuarios.nombre
    var direccion by Usuarios.direccion
    var telefono by Usuarios.telefono
    var email by Usuarios.email
    var contraseña by Usuarios.contraseña
}

