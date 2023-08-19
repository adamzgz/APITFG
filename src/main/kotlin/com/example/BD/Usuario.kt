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
    val imagen = varchar("imagen", 255).nullable()  // Añadido
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
            val imagen: String? = null  // Añadido
        )

        @Serializable
        data class EmpleadoDto(
            val nombre: String,
            val direccion: String,
            val telefono: String,
            val email: String,
            val contraseña: String,
            val rol: String,
            val imagen: String? = null  // Añadido
        )

        fun crearUsuario(usuarioDto: UsuarioDto): Boolean {
            if (usuarioDto.nombre.isBlank() || usuarioDto.direccion.isBlank() || usuarioDto.telefono.isBlank() || usuarioDto.email.isBlank() || usuarioDto.contraseña.isBlank()) {
                println("Datos del usuario no válidos.")
                return false
            }

            return registrar(usuarioDto)
        }

        fun eliminarUsuario(id: Int): Boolean {
            if (id <= 0) {
                println("ID de usuario no válido.")
                return false
            }

            return transaction {
                try {
                    val usuario = Usuario.findById(id)
                    if (usuario != null) {
                        val cliente = Cliente.find { Clientes.id_usuario eq usuario.id }.singleOrNull()
                        val empleado = Empleado.find { Empleados.id_usuario eq usuario.id }.singleOrNull()

                        cliente?.delete()
                        empleado?.delete()

                        usuario.delete()
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
                        usuario.imagen = usuarioDto.imagen  // Añadido
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
                Conexion.conectar()
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
                        imagen = usuarioDto.imagen  // Añadido
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
    var imagen by Usuarios.imagen  // Añadido
}
