import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.example.plugins.configureRouting
import io.ktor.server.application.*


fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}
fun Application.module() {
    Usuario.verificarAdministrador()
    configureRouting()

}