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
                route("/categorias") {
                    post {
                        val categoriaDto = call.receive<Categoria.Companion.CategoriaDto>()
                        val success = Categoria.crearCategoria(categoriaDto.nombre)
                        if (success) call.respond(HttpStatusCode.Created)
                        else call.respond(HttpStatusCode.BadRequest, "No se pudo crear la categoría")
                    }

                    get {
                        val categorias = Categoria.obtenerCategorias()
                        call.respond(categorias)
                    }

                    get("/{id}") {
                        val id = call.parameters["id"]?.toIntOrNull()
                        val categoria = id?.let { Categoria.obtenerCategoria(it) }
                        if (categoria != null) call.respond(categoria)
                        else call.respond(HttpStatusCode.NotFound)
                    }

                    put("/{id}") {
                        val categoriaDto = call.receive<Categoria.Companion.CategoriaDto>()
                        val id = call.parameters["id"]?.toIntOrNull()
                        if (id != null && Categoria.actualizarCategoria(id, categoriaDto.nombre)) {
                            call.respond(HttpStatusCode.OK)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }

                    delete("/{id}") {
                        val id = call.parameters["id"]?.toIntOrNull()
                        if (id != null && Categoria.eliminarCategoria(id)) {
                            call.respond(HttpStatusCode.NoContent)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }
                }
                route("/chats") {
                    post {
                        val chatDto = call.receive<Chat.Companion.ChatDto>()
                        val chat = Chat.crearChat(chatDto.idPedido)
                        if (chat != null) {
                            call.respond(HttpStatusCode.Created)
                        } else {
                            call.respond(HttpStatusCode.BadRequest, "No se pudo crear el chat")
                        }
                    }

                    get {
                        val chats = Chat.obtenerChats()
                        call.respond(chats)
                    }

                    get("/{id}") {
                        val id = call.parameters["id"]?.toIntOrNull()
                        val chat = id?.let { Chat.obtenerChat(it) }
                        if (chat != null) {
                            call.respond(chat)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }

                    put("/{id}") {
                        val chatDto = call.receive<Chat.Companion.ChatDto>()
                        val id = call.parameters["id"]?.toIntOrNull()
                        val success = id?.let { Chat.actualizarEstadoChat(it, chatDto.estado) }
                        if (success == true) {
                            call.respond(HttpStatusCode.OK)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }

                    delete("/{id}") {
                        val id = call.parameters["id"]?.toIntOrNull()
                        val success = id?.let { Chat.borrarChat(it) }
                        if (success == true) {
                            call.respond(HttpStatusCode.NoContent)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }
                }
                route("/clientes") {
                    post {
                        val clienteDto = call.receive<Cliente.Companion.ClienteDto>()
                        val success = Cliente.crearCliente(clienteDto.idUsuario, clienteDto.vip)
                        if (success) {
                            call.respond(HttpStatusCode.Created)
                        } else {
                            call.respond(HttpStatusCode.BadRequest, "No se pudo crear el cliente")
                        }
                    }

                    get {
                        val clientes = Cliente.obtenerClientes()
                        call.respond(clientes)
                    }

                    get("/{id}") {
                        val id = call.parameters["id"]?.toIntOrNull()
                        val cliente = id?.let { Cliente.obtenerCliente(it) }
                        if (cliente != null) {
                            call.respond(cliente)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }

                    put("/{id}") {
                        val clienteDto = call.receive<Cliente.Companion.ClienteDto>()
                        val id = call.parameters["id"]?.toIntOrNull()
                        val success = id?.let {
                            Cliente.actualizarCliente(
                                it,
                                clienteDto.idUsuario,
                                clienteDto.vip
                            )
                        }
                        if (success == true) {
                            call.respond(HttpStatusCode.OK)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }

                    delete("/{id}") {
                        val id = call.parameters["id"]?.toIntOrNull()
                        val success = id?.let { Cliente.eliminarCliente(it) }
                        if (success == true) {
                            call.respond(HttpStatusCode.NoContent)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }
                }
                route("/detalles-pedidos") {
                    post {
                        val detallePedidoDto = call.receive<DetallePedido.Companion.DetallePedidoDto>()
                        DetallePedido.insertDetallePedido(
                            detallePedidoDto.idPedido,
                            detallePedidoDto.idProducto,
                            detallePedidoDto.cantidad
                        )
                        call.respond(HttpStatusCode.Created)
                    }

                    delete("/{id}") {
                        val id = call.parameters["id"]?.toIntOrNull()
                        val success = id?.let { DetallePedido.borrarDetallePedido(it) }
                        if (success == true) {
                            call.respond(HttpStatusCode.NoContent)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }

                    put("/{id}") {
                        val detallePedidoDto = call.receive<DetallePedido.Companion.DetallePedidoDto>()
                        val id = call.parameters["id"]?.toIntOrNull()
                        val success = id?.let {
                            DetallePedido.actualizarCantidadDetallePedido(
                                it,
                                detallePedidoDto.cantidad
                            )
                        }
                        if (success == true) {
                            call.respond(HttpStatusCode.OK)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }
                }
                route("/empleados") {
                    post {
                        val empleadoDto = call.receive<Empleado.Companion.EmpleadoDto>()
                        val success = Empleado.crearEmpleado(empleadoDto.idUsuario, empleadoDto.rol)
                        if (success) {
                            call.respond(HttpStatusCode.Created)
                        } else {
                            call.respond(HttpStatusCode.BadRequest, "No se pudo crear el empleado")
                        }
                    }

                    get {
                        val empleados = Empleado.obtenerEmpleados()
                        call.respond(empleados)
                    }

                    get("/{id}") {
                        val id = call.parameters["id"]?.toIntOrNull()
                        val empleado = id?.let { Empleado.obtenerEmpleado(it) }
                        if (empleado != null) {
                            call.respond(empleado)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }

                    put("/{id}") {
                        val empleadoDto = call.receive<Empleado.Companion.EmpleadoDto>()
                        val id = call.parameters["id"]?.toIntOrNull()
                        val success = id?.let {
                            Empleado.actualizarEmpleado(
                                it,
                                empleadoDto.idUsuario,
                                empleadoDto.rol
                            )
                        }
                        if (success == true) {
                            call.respond(HttpStatusCode.OK)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }

                    delete("/{id}") {
                        val id = call.parameters["id"]?.toIntOrNull()
                        val success = id?.let { Empleado.eliminarEmpleado(it) }
                        if (success == true) {
                            call.respond(HttpStatusCode.NoContent)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }
                }
                route("/empleados-chats") {
                    post {
                        val empleadoChatDto = call.receive<EmpleadoChat.Companion.EmpleadoChatDto>()
                        EmpleadoChat.insertEmpleadoChat(empleadoChatDto.idEmpleado, empleadoChatDto.idChat)
                        call.respond(HttpStatusCode.Created)
                    }

                    delete("/{id}") {
                        val id = call.parameters["id"]?.toIntOrNull()
                        val success = id?.let { EmpleadoChat.borrarEmpleadoChat(it) }
                        if (success == true) {
                            call.respond(HttpStatusCode.NoContent)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }

                    put("/{id}") {
                        val empleadoChatDto = call.receive<EmpleadoChat.Companion.EmpleadoChatDto>()
                        val id = call.parameters["id"]?.toIntOrNull()
                        val success = id?.let {
                            EmpleadoChat.actualizarEmpleadoChat(
                                it,
                                empleadoChatDto.idEmpleado,
                                empleadoChatDto.idChat
                            )
                        }
                        if (success == true) {
                            call.respond(HttpStatusCode.OK)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }


                }
                route("/mensajes") {
                    post {
                        val mensajeDto = call.receive<Mensaje.Companion.MensajeDto>()
                        val success = Mensaje.crearMensaje(
                            mensajeDto.idChat,
                            mensajeDto.idUsuario,
                            mensajeDto.mensaje
                        )
                        if (success) {
                            call.respond(HttpStatusCode.Created)
                        } else {
                            call.respond(HttpStatusCode.BadRequest, "No se pudo crear el mensaje")
                        }
                    }

                    delete("/{id}") {
                        val id = call.parameters["id"]?.toIntOrNull()
                        val success = id?.let { Mensaje.borrarMensaje(it) }
                        if (success == true) {
                            call.respond(HttpStatusCode.NoContent)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }

                    put("/{id}") {
                        val mensajeDto = call.receive<Mensaje.Companion.MensajeDto>()
                        val id = call.parameters["id"]?.toIntOrNull()
                        val success = id?.let {
                            Mensaje.actualizarMensaje(
                                it,
                                mensajeDto.mensaje
                            )
                        }
                        if (success == true) {
                            call.respond(HttpStatusCode.OK)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }

                    get("/chat/{idChat}") {
                        val idChat = call.parameters["idChat"]?.toIntOrNull()
                        if (idChat != null) {
                            val mensajes = Mensaje.obtenerMensajesDeChat(idChat)
                            call.respond(mensajes)
                        } else {
                            call.respond(HttpStatusCode.BadRequest, "ID de chat inválido")
                        }
                    }
                }
                route("/pedidos") {
                    post {
                        val pedidoDto = call.receive<Pedido.Companion.PedidoDto>()
                        val estadoPedido = Pedidos.EstadoPedido.valueOf(pedidoDto.estado)
                        val success = Pedido.crearPedido(pedidoDto.idCliente, estadoPedido)
                        if (success) {
                            call.respond(HttpStatusCode.Created)
                        } else {
                            call.respond(HttpStatusCode.BadRequest, "No se pudo crear el pedido")
                        }
                    }

                    delete("/{id}") {
                        val id = call.parameters["id"]?.toIntOrNull()
                        val success = id?.let { Pedido.borrarPedido(it) }
                        if (success == true) {
                            call.respond(HttpStatusCode.NoContent)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }

                    put("/{id}") {
                        val pedidoDto = call.receive<Pedido.Companion.PedidoDto>()
                        val id = call.parameters["id"]?.toIntOrNull()
                        val success = id?.let {
                            Pedido.actualizarPedido(
                                it,
                                Pedidos.EstadoPedido.valueOf(pedidoDto.estado)
                            )
                        }
                        if (success == true) {
                            call.respond(HttpStatusCode.OK)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }

                    get {
                        val pedidos = Pedido.obtenerPedidos()
                        call.respond(pedidos)
                    }
                }
                route("/productos") {
                    post {
                        val productoDto = call.receive<Producto.Companion.ProductoDto>()
                        val success = Producto.crearProducto(
                            productoDto.nombre,
                            productoDto.descripcion,
                            productoDto.precio,
                            productoDto.stock,
                            productoDto.idCategoria,
                            productoDto.imagen
                        )
                        if (success) {
                            call.respond(HttpStatusCode.Created)
                        } else {
                            call.respond(HttpStatusCode.BadRequest, "No se pudo crear el producto")
                        }
                    }

                    delete("/{id}") {
                        val id = call.parameters["id"]?.toIntOrNull()
                        val success = id?.let { Producto.borrarProducto(it) }
                        if (success == true) {
                            call.respond(HttpStatusCode.NoContent)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }

                    put("/{id}") {
                        val productoDto = call.receive<Producto.Companion.ProductoDto>()
                        val id = call.parameters["id"]?.toIntOrNull()
                        val success = id?.let {
                            Producto.actualizarProducto(
                                it,
                                productoDto.nombre,
                                productoDto.descripcion,
                                productoDto.precio,
                                productoDto.stock,
                                productoDto.idCategoria,
                                productoDto.imagen
                            )
                        }
                        if (success == true) {
                            call.respond(HttpStatusCode.OK)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }

                    get {
                        val productos = Producto.obtenerProductos()
                        call.respond(productos)
                    }
                }
                route("/valoraciones") {
                    post {
                        val valoracionDto = call.receive<Valoracion.Companion.ValoracionDto>()
                        val success = Valoracion.crearValoracion(
                            valoracionDto.idCliente,
                            valoracionDto.idProducto,
                            valoracionDto.puntuacion,
                            valoracionDto.comentario
                        )
                        if (success) {
                            call.respond(HttpStatusCode.Created)
                        } else {
                            call.respond(HttpStatusCode.BadRequest, "No se pudo crear la valoración")
                        }
                    }

                    delete("/{id}") {
                        val id = call.parameters["id"]?.toIntOrNull()
                        val success = id?.let { Valoracion.borrarValoracion(it) }
                        if (success == true) {
                            call.respond(HttpStatusCode.NoContent)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }

                    put("/{id}") {
                        val valoracionDto = call.receive<Valoracion.Companion.ValoracionDto>()
                        val id = call.parameters["id"]?.toIntOrNull()
                        val success = id?.let {
                            Valoracion.actualizarValoracion(
                                it,
                                valoracionDto.puntuacion,
                                valoracionDto.comentario
                            )
                        }
                        if (success == true) {
                            call.respond(HttpStatusCode.OK)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }

                    get("/puntuacion-media/{idProducto}") {
                        val idProducto = call.parameters["idProducto"]?.toIntOrNull()
                        if (idProducto != null) {
                            val puntuacionMedia = Valoracion.obtenerPuntuacionMediaPorProducto(idProducto)
                            if (puntuacionMedia != null) {
                                call.respond(puntuacionMedia)
                            } else {
                                call.respond(HttpStatusCode.NotFound)
                            }
                        } else {
                            call.respond(HttpStatusCode.BadRequest, "ID de producto inválido")
                        }
                    }
                }
            }
        }
    }
}


