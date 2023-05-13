import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.`java-time`.datetime

object Valoraciones : IntIdTable() {
    val id_cliente = reference("id_cliente", Clientes)
    val id_producto = reference("id_producto", Productos)
    val puntuacion = integer("puntuacion")
    val comentario = text("comentario").nullable()
    val fecha_valoracion = datetime("fecha_valoracion")
}

class Valoracion(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Valoracion>(Valoraciones)

    var id_cliente by Cliente referencedOn Valoraciones.id_cliente
    var id_producto by Producto referencedOn Valoraciones.id_producto
    var puntuacion by Valoraciones.puntuacion
    var comentario by Valoraciones.comentario
    var fecha_valoracion by Valoraciones.fecha_valoracion
}
