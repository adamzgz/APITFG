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
            val contraseña: String,

        )

        @Serializable
        data class EmpleadoDto(
            val nombre: String,
            val direccion: String,
            val telefono: String,
            val email: String,
            val contraseña: String,
            val rol: String,

        )

        fun crearUsuario(usuarioDto: UsuarioDto): Boolean {
            if (usuarioDto.nombre.isBlank() || usuarioDto.direccion.isBlank() || usuarioDto.telefono.isBlank() || usuarioDto.email.isBlank() || usuarioDto.contraseña.isBlank()) {
                println("Datos del usuario no válidos.")
                return false
            }

            return registrar(usuarioDto)
        }

        fun borrarUsuario(id: Int): Boolean {
            // Validar id
            if (id <= 0) {
                println("El ID del usuario no es válido.")
                return false
            }

            return transaction {
                try {
                    // Buscar el usuario por su ID
                    val usuario = Usuario.findById(id)
                    if (usuario == null) {
                        println("El usuario con ID $id no existe.")
                        return@transaction false
                    }

                    // Comprobar si es un cliente y, si es así, eliminarlo
                    val cliente = Cliente.obtenerClientePorUsuario(id)
                    cliente?.delete()

                    // Comprobar si es un empleado y, si es así, eliminarlo
                    val empleado = Empleado.find { Empleados.id_usuario eq id }.singleOrNull()
                    empleado?.delete()

                    // Finalmente, eliminar el usuario
                    usuario.delete()

                    println("Usuario borrado con éxito.")
                    return@transaction true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@transaction false
                }
            }
        }
        fun tipoDeUsuario(id: Int): String {
            return transaction {
                when {
                    Cliente.obtenerClientePorUsuario(id) != null -> "Cliente"
                    Empleado.find { Empleados.id_usuario eq id }.singleOrNull() != null -> "Empleado"
                    else -> "Desconocido"
                }
            }
        }



        fun actualizarUsuario(id: Int, usuarioDto: UsuarioDto): Boolean {
            if (id <= 0 || usuarioDto.nombre.isBlank() || usuarioDto.direccion.isBlank() || usuarioDto.telefono.isBlank() || usuarioDto.email.isBlank() || usuarioDto.contraseña.isBlank()) {
                println("Datos del usuario no válidos.")
                return false
            }

            return transaction {
                try {
                    val usuario = Usuario.findById(id)
                    if (usuario != null) {
                        usuario.nombre = usuarioDto.nombre
                        usuario.direccion = usuarioDto.direccion
                        usuario.telefono = usuarioDto.telefono
                        usuario.email = usuarioDto.email
                        usuario.contraseña = cifrarContraseña(usuarioDto.contraseña)

                        return@transaction true
                    } else {
                        println("No se pudo encontrar el usuario con ID: $id")
                        return@transaction false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@transaction false
                }
            }
        }

        fun esAdministrador(id: Int): Boolean {
            if (id <= 0) {
                println("ID de usuario no válido.")
                return false
            }

            return transaction {
                val usuario = Usuario.findById(id)
                val empleado = Empleado.find { Empleados.id_usuario eq id }
                empleado.any { it.rol == Empleados.RolEmpleado.ADMINISTRADOR }
            }
        }

        private fun cifrarContraseña(contraseña: String): String {
            return BCrypt.hashpw(contraseña, BCrypt.gensalt())
        }

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

                    Cliente.new {
                        id_usuario = nuevoUsuario
                        vip = false
                    }

                    return@transaction true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@transaction false
                }
            }
        }

        private fun validarEmailUnico(email: String) {
            if (Usuario.find { Usuarios.email eq email }.count() > 0) {
                throw IllegalArgumentException("El email ya está registrado")
            }
        }

        private fun validarTelefonoUnico(telefono: String) {
            if (Usuario.find { Usuarios.telefono eq telefono }.count() > 0) {
                throw IllegalArgumentException("El teléfono ya está registrado")
            }
        }

        private fun validarContraseña(contraseña: String) {
            val regex = Regex("^(?=.*[A-Z])(?=.*[0-9]).+$")
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
