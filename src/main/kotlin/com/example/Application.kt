package com.example
import Usuario
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.example.plugins.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json() // Instala el     soporte para JSON utilizando kotlinx.serialization
    }

    install(Authentication) {
        jwt {
            realm = "SecureRealm"
            verifier(makeJwtVerifier())
            validate {
                val name = it.payload.getClaim("name").asString()
                val password = it.payload.getClaim("password").asString()
                if (name != null && password != null) {
                    UserIdPrincipal(name)
                } else null
            }
        }
    }

    install(Routing) {
        route("/auth") {
            post("/register") {
                val user = call.receive<Usuario>()
                val registrado = user.registrar()
                if (registrado) {
                    call.respond(HttpStatusCode.Created)
                } else {
                    call.respond(HttpStatusCode.BadRequest, "No se pudo registrar el usuario")
                }
            }

            authenticate {
                post("/login") {
                    val post = call.receive<Post>()
                    // Aquí deberías tener la lógica para verificar la información de inicio de sesión del usuario y emitir un token
                    call.respond(HttpStatusCode.OK, "ERRORSUTI")
                }
            }
        }
        this@module.configureRouting()
    }

}



// Función ficticia que deberías implementar para verificar los JWTs.
// Asegúrate de reemplazar esta con tu propia implementación.
fun makeJwtVerifier(): JWTVerifier {
    return JWT.require(Algorithm.HMAC256("mySecret")).withIssuer("ktor.io").build()
}

// Clase ficticia para los objetos Post recibidos.
// Asegúrate de reemplazar esta con tu propia implementación.
class Post
