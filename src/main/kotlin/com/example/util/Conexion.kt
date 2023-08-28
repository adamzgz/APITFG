import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

class Conexion {
    companion object {
        private const val DB_URL = "jdbc:mysql://localhost:3306/proyectofingrado"
        private const val DB_USER = "root"
        private const val DB_PASSWORD = "root"

        fun conectar() {
            Database.connect(DB_URL, driver = "com.mysql.jdbc.Driver", user = DB_USER, password = DB_PASSWORD)
            transaction {
                SchemaUtils.create(
                    Usuarios,
                    Clientes,
                    Empleados,
                    Productos,
                    Pedidos,
                    DetallesPedidos,
                    Valoraciones,
                    Categorias
                )
            }
        }
    }
}
