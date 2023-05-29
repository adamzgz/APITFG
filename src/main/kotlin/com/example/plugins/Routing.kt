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
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.util.*

@Serializable
data class Post(
    val email: String,
    val contraseña: String
)

fun Application.configureRouting() {
    install(ContentNegotiation) {
        json()
    }
    val secret = "secreto"
    val issuer = "127.0.0.1"
    val jwtRealm = "MyAppSecureRealm"

    install(Authentication) {
        jwt("jwt-auth") {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(secret))
                    .withIssuer(issuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("email").asString() != null) {
                    JWTPrincipal(credential.payload)
                } else null
            }
        }
    }

    routing {
        route("/auth") {
            post("/register") {
                val usuarioDto = call.receive<Usuario.Companion.UsuarioDto>()
                Conexion.conectar()

                val registrado = Usuario.registrar(usuarioDto)
                if (registrado) {
                    call.respond(HttpStatusCode.Created)
                } else {
                    call.respond(HttpStatusCode.BadRequest, "No se pudo registrar el usuario")
                }
            }
            post("/login") {
                val post = call.receive<Usuario.Companion.UsuarioDto>()
                Conexion.conectar()
                if (post.email.isEmpty() || post.contraseña.isEmpty()) {
                    call.respond(HttpStatusCode.Unauthorized, "Contraseña o email incorrectos")
                } else {
                    val user = transaction { Usuario.find { Usuarios.email eq post.email }.singleOrNull() }
                    if (user != null && BCrypt.checkpw(post.contraseña, user.contraseña)) {
                        val token = JWT.create()
                            .withSubject("Authentication")
                            .withIssuer(issuer)
                            .withClaim("email", post.email)
                            .withClaim("userId", user.id.toString())  // Agregar la ID del usuario a las claims
                            .withExpiresAt(Date(System.currentTimeMillis() + 86_400_000)) // 24 hour validity
                            .sign(Algorithm.HMAC256(secret))

                        // Busca en las tablas de Cliente y Empleado para verificar la existencia del ID de usuario
                        val isClient = transaction { Cliente.find { Clientes.id_usuario eq user.id }.any() }
                        val isEmployee = transaction { Empleado.find { Empleados.id_usuario eq user.id }.any() }

                        // Asegúrate de manejar el caso en el que un ID de usuario pueda ser tanto un cliente como un empleado,
                        // dependiendo de cómo esté diseñada tu base de datos y lógica de negocio
                        if (isClient) {
                            call.respond(hashMapOf("token" to token, "role" to "Cliente"))
                        } else if (isEmployee) {
                            call.respond(hashMapOf("token" to token, "role" to "Empleado"))
                        } else {
                            call.respond(HttpStatusCode.Unauthorized, "No se pudo determinar el rol del usuario")
                        }
                    } else {
                        call.respond(HttpStatusCode.Unauthorized, "Contraseña o email incorrectos")
                    }
                }
            }
        }
        authenticate("jwt-auth") {
            route("/secure") {
                get("/") {
                    val principal = call.principal<JWTPrincipal>()
                    val email = principal!!.payload.getClaim("email").asString()
                    val subject = principal.payload.subject
                    call.respondText("Authenticated: $email \n $subject")
                }
            }
        }
    }
}
