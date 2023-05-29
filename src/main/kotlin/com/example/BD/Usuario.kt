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
    companion object : IntEntityClass<Usuario>(Usuarios)

    var nombre by Usuarios.nombre
    var direccion by Usuarios.direccion
    var telefono by Usuarios.telefono
    var email by Usuarios.email
    var contraseña by Usuarios.contraseña

    @Serializable
    data class UsuarioDto(
        val nombre: String,
        val direccion: String,
        val telefono: String,
        val email: String,
        val contraseña: String
    )

    init {
        validarEmailUnico()
        validarTelefonoUnico()
        //validarContraseña()
    }

    private fun validarEmailUnico() {
        if (transaction { Usuario.find { Usuarios.email eq email }.count() > 0 }) {
            throw IllegalArgumentException("El email ya está registrado")
        }
    }

    private fun validarTelefonoUnico() {
        if (transaction { Usuario.find { Usuarios.telefono eq telefono }.count() > 0 }) {
            throw IllegalArgumentException("El teléfono ya está registrado")
        }
    }

    /*private fun validarContraseña() {
        val regex = Regex("^(?=.*[A-Z])(?=.*[0-9]).+\$")
        if (!contraseña.matches(regex)) {
            throw IllegalArgumentException("La contraseña debe contener al menos una letra mayúscula y un número")
        }
    }*/

    fun registrar(): Boolean {
        return transaction {
            try {
                val contraseñaCifrada = cifrarContraseña(contraseña)

                val nuevoUsuario = Usuario.new {
                    this.nombre = this@Usuario.nombre
                    this.direccion = this@Usuario.direccion
                    this.telefono = this@Usuario.telefono
                    this.email = this@Usuario.email
                    this.contraseña = contraseñaCifrada
                }

                // Crear instancia en la tabla "Clientes"
                Cliente.new {
                    this.id_usuario = nuevoUsuario
                    this.vip = false
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
}
