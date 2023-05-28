import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

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
    companion object : IntEntityClass<Empleado>(Empleados)

    var id_usuario by Usuario referencedOn Empleados.id_usuario
    var rol by Empleados.rol
}
