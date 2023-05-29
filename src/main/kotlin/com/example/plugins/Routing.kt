package com.example.plugins

import Conexion
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import org.jetbrains.exposed.sql.transactions.transaction


data class Post(
    val email: String,
    val contraseña: String
)

fun Application.configureRouting() {
    install(ContentNegotiation) {
        json()
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

    routing {
        route("/auth") {
            post("/register") {
                val usuarioDto = call.receive<Usuario.UsuarioDto>()
                Conexion.conectar()
                transaction {
                    val usuario = Usuario.new {
                        nombre = usuarioDto.nombre
                        direccion = usuarioDto.direccion
                        telefono = usuarioDto.telefono
                        email = usuarioDto.email
                        contraseña = usuarioDto.contraseña
                    }
                }
                   /* val registrado = usuario.registrar()
                    if (registrado) {
                        call.respond(HttpStatusCode.Created)
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "No se pudo registrar el usuario")
                    }

                    */
                }

            post("/login") {
                val post = call.receive<Post>()
                // Aquí deberías tener la lógica para verificar la información de inicio de sesión del usuario y emitir un token
                call.respond(HttpStatusCode.OK, "ERRORSUTI")
            }

            authenticate {
                get("/") {
                    call.respond(HttpStatusCode.BadRequest, "ERROR")
                }
            }
        }
    }
}

fun makeJwtVerifier(): JWTVerifier {
    return JWT.require(Algorithm.HMAC256("mySecret")).withIssuer("ktor.io").build()
}
