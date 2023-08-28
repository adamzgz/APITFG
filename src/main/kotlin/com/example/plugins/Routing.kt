package com.example.plugins

import Conexion
import Producto.Companion.crearProducto
import Usuario
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlinx.serialization.Serializable
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

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
    fun obtenerIdUsuarioDesdeToken(call: ApplicationCall): Int? {
        val principal = call.authentication.principal<JWTPrincipal>()
        println(principal?.payload?.getClaim("userId")?.asInt())
        return principal?.payload?.getClaim("userId")?.asInt()
    }

    routing {
        // Servir imágenes desde la carpeta resources/img_productos/
        static("/img_productos") {
            resources("img_productos/")
        }
        // Servir imágenes desde la carpeta resources/img_usuarios/
        static("/img_usuarios") {
            resources("img_usuarios/")
        }

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
                            .withClaim("userId", user.id?.value)  // Agregar la ID del usuario a las claims
                            .withExpiresAt(Date(System.currentTimeMillis() + 86_400_000)) // 24 hour validity
                            .sign(Algorithm.HMAC256(secret))

                        // Busca en las tablas de Cliente y Empleado para verificar la existencia del ID de usuario
                        val isClient = transaction { Cliente.find { Clientes.id_usuario eq user.id }.any() }
                        val isEmployee = transaction { Empleado.find { Empleados.id_usuario eq user.id }.any() }

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
                route("/add_img_productos") {
                    post {
                        println("Entrando al endpoint /add_img_productos")

                        val multipart = call.receiveMultipart()
                        var imageSavedPath: String? = null

                        multipart.forEachPart { part ->
                            when (part) {
                                is PartData.FileItem -> {
                                    val originalFileName = part.originalFileName!!
                                    println("Archivo recibido: $originalFileName")

                                    // Comprobamos si hay un producto con el mismo nombre de imagen
                                    val existingProductWithImage = transaction {
                                        Producto.find { Productos.imagen eq originalFileName }.singleOrNull()
                                    }

                                    if (existingProductWithImage != null) {
                                        call.respond(HttpStatusCode.Conflict, "Ya hay un producto con una imagen con ese nombre.")
                                        part.dispose()
                                        return@forEachPart
                                    }

                                    val ext = File(originalFileName).extension
                                    println("Extensión del archivo: $ext")

                                    val file = File("D:\\Descargas\\proyectoTFG\\src\\main\\resources\\img_productos\\${part.originalFileName}")
                                    println("Ruta donde se guardará la imagen: ${file.absolutePath}")
                                    imageSavedPath = file.absolutePath

                                    part.streamProvider().use { its ->
                                        file.outputStream().buffered().use {
                                            val bytesCopied = its.copyToSuspend(it)
                                            println("Se copiaron $bytesCopied bytes al archivo.")
                                        }
                                    }
                                }
                                else -> {
                                    println("Tipo de parte no reconocido: ${part.name}")
                                }
                            }
                            part.dispose()
                        }

                        if (imageSavedPath != null) {
                            call.respond(HttpStatusCode.OK, mapOf("path" to imageSavedPath!!))
                        } else {
                            call.respond(HttpStatusCode.BadRequest, "No se proporcionó una imagen válida.")
                        }
                    }
                }

                route("/categorias") {
                    post {
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)
                        if (idUsuario != null && Usuario.esAdministrador(idUsuario)) {
                            val categoriaDto = call.receive<Categoria.Companion.CategoriaDto>()
                            val success = Categoria.crearCategoria(categoriaDto.nombre)
                            if (success) call.respond(HttpStatusCode.Created)
                            else call.respond(HttpStatusCode.BadRequest, "No se pudo crear la categoría")
                        } else {
                            call.respond(HttpStatusCode.Forbidden)
                        }
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
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)
                        if (idUsuario != null && Usuario.esAdministrador(idUsuario)) {
                            val categoriaDto = call.receive<Categoria.Companion.CategoriaDto>()
                            val id = call.parameters["id"]?.toIntOrNull()
                            if (id != null && Categoria.actualizarCategoria(id, categoriaDto.nombre)) {
                                call.respond(HttpStatusCode.OK)
                            } else {
                                call.respond(HttpStatusCode.NotFound)
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden)
                        }
                    }

                    delete("/{id}") {
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)
                        if (idUsuario != null && Usuario.esAdministrador(idUsuario)) {
                            val id = call.parameters["id"]?.toIntOrNull()
                            if (id != null && Categoria.eliminarCategoria(id)) {
                                call.respond(HttpStatusCode.NoContent)
                            } else {
                                call.respond(HttpStatusCode.NotFound)
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden)
                        }
                    }
                }

                /*route("/chats") {
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
                }*/
                route("/clientes") {
                    post {
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)
                        val clienteDto = call.receive<Cliente.Companion.ClienteDto>()
                        if (idUsuario != null && (Usuario.esAdministrador(idUsuario) || idUsuario == clienteDto.idUsuario)) {
                            val success = Cliente.crearCliente(clienteDto.idUsuario, clienteDto.vip)
                            if (success) {
                                call.respond(HttpStatusCode.Created)
                            } else {
                                call.respond(HttpStatusCode.BadRequest, "No se pudo crear el cliente")
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden)
                        }
                    }

                    get {
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)
                        if (idUsuario != null) {
                            val clientes = Cliente.obtenerClientes()
                                .filter { it.idUsuario == idUsuario || Usuario.esAdministrador(idUsuario) }
                            call.respond(clientes)
                        } else {
                            call.respond(HttpStatusCode.Forbidden)
                        }
                    }

                    get("/{id}") {
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)
                        val id = call.parameters["id"]?.toIntOrNull()
                        val cliente = id?.let { Cliente.obtenerCliente(it) }
                        if (idUsuario != null && (cliente?.idUsuario == idUsuario || Usuario.esAdministrador(idUsuario))) {
                            if (cliente != null) {
                                call.respond(cliente)
                            } else {
                                call.respond(HttpStatusCode.NotFound)
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden)
                        }
                    }

                    put("/{id}") {
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)
                        val id = call.parameters["id"]?.toIntOrNull()
                        val clienteDto = call.receive<Cliente.Companion.ClienteDto>()
                        if (id != null && idUsuario != null && (idUsuario == clienteDto.idUsuario || Usuario.esAdministrador(
                                idUsuario
                            ))
                        ) {
                            val success = Cliente.actualizarCliente(id, clienteDto.idUsuario, clienteDto.vip)
                            if (success) {
                                call.respond(HttpStatusCode.OK)
                            } else {
                                call.respond(HttpStatusCode.BadRequest, "No se pudo actualizar el cliente")
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden)
                        }
                    }

                    delete("/{id}") {
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)
                        val id = call.parameters["id"]?.toIntOrNull()
                        val cliente = id?.let { Cliente.obtenerCliente(it) }
                        if (idUsuario != null && (cliente?.idUsuario == idUsuario || Usuario.esAdministrador(idUsuario))) {
                            val success = id?.let { it1 -> Cliente.eliminarCliente(it1) }
                            if (success == true) {
                                call.respond(HttpStatusCode.NoContent)
                            } else {
                                call.respond(HttpStatusCode.BadRequest, "No se pudo eliminar el cliente")
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden)
                        }
                    }
                }


                route("/detalles-pedidos") {
                    post {
                        val detallePedidoDto = call.receive<DetallePedido.Companion.DetallePedidoDto>()
                        DetallePedido.insertDetallePedido(
                            detallePedidoDto.idPedido!!,
                            detallePedidoDto.idProducto!!,
                            detallePedidoDto.cantidad
                        )
                        call.respond(HttpStatusCode.Created)
                    }
                    get("/{idPedido}") {
                        val idPedido = call.parameters["idPedido"]?.toIntOrNull()
                        if (idPedido != null) {
                            val detalles = DetallePedido.obtenerDetallesPorPedido(idPedido)
                            if (detalles.isNotEmpty()) {
                                call.respond(HttpStatusCode.OK, detalles)
                            } else {
                                call.respond(HttpStatusCode.NotFound)
                            }
                        } else {
                            call.respond(HttpStatusCode.BadRequest)
                        }
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
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)
                        val empleadoDto = call.receive<Empleado.Companion.EmpleadoDto>()
                        if (idUsuario != null && Usuario.esAdministrador(idUsuario)) {
                            val success = Empleado.crearEmpleado(empleadoDto.idUsuario, empleadoDto.rol)
                            if (success) {
                                call.respond(HttpStatusCode.Created)
                            } else {
                                call.respond(HttpStatusCode.BadRequest, "No se pudo crear el empleado")
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden)
                        }
                    }

                    get {
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)
                        if (idUsuario != null && Usuario.esAdministrador(idUsuario)) {
                            val empleados = Empleado.obtenerEmpleados()
                            call.respond(empleados)
                        } else {
                            call.respond(HttpStatusCode.Forbidden)
                        }
                    }

                    get("/{id}") {
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)
                        val id = call.parameters["id"]?.toIntOrNull()
                        val empleado = id?.let { Empleado.obtenerEmpleado(it) }
                        if (idUsuario != null && Usuario.esAdministrador(idUsuario)) {
                            if (empleado != null) {
                                call.respond(empleado)
                            } else {
                                call.respond(HttpStatusCode.NotFound)
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden)
                        }
                    }

                    put("/{id}") {
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)
                        val id = call.parameters["id"]?.toIntOrNull()
                        val empleadoDto = call.receive<Empleado.Companion.EmpleadoDto>()
                        if (id != null && idUsuario != null && Usuario.esAdministrador(idUsuario)) {
                            val success = Empleado.actualizarEmpleado(id, empleadoDto.idUsuario, empleadoDto.rol)
                            if (success) {
                                call.respond(HttpStatusCode.OK)
                            } else {
                                call.respond(HttpStatusCode.BadRequest, "No se pudo actualizar el empleado")
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden)
                        }
                    }

                    delete("/{id}") {
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)
                        val id = call.parameters["id"]?.toIntOrNull()
                        if (id != null && idUsuario != null && Usuario.esAdministrador(idUsuario)) {
                            val success = Empleado.eliminarEmpleado(id)
                            if (success) {
                                call.respond(HttpStatusCode.NoContent)
                            } else {
                                call.respond(HttpStatusCode.BadRequest, "No se pudo eliminar el empleado")
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden)
                        }
                    }
                }
                /*route("/empleados-chats") {
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


                }*/
               /* route("/mensajes") {
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
                }*/
                route("/pedidos") {
                    post {
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)
                        val cliente = Cliente.obtenerClientePorUsuario(idUsuario!!)
                        if (cliente != null) {
                            val estadoPedido = Pedidos.EstadoPedido.EN_PROCESO
                            val pedidoCreado = Pedido.crearPedido(cliente.id.value, estadoPedido)
                            if (pedidoCreado != null) {
                                // Convert the Exposed entity to a DTO object
                                val pedidoDto = Pedido.Companion.PedidoDto(
                                    idPedido = pedidoCreado.id.value,
                                    idCliente = pedidoCreado.id_cliente.value,
                                    estado = pedidoCreado.estado.name
                                )
                                // Use the DTO object in the response
                                call.respond(HttpStatusCode.Created, pedidoDto)
                            } else {
                                call.respond(HttpStatusCode.BadRequest, "No se pudo crear el pedido")
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden)
                        }
                    }



                    get {
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)
                        if (Usuario.esAdministrador(idUsuario!!)) {
                            val pedidos = Pedido.obtenerPedidos()
                            call.respond(pedidos)
                        } else {
                            val cliente = Cliente.obtenerClientePorUsuario(idUsuario)
                            if (cliente != null) {
                                val pedidos = Pedido.obtenerPedidosPorUsuario(cliente.id.value)
                                call.respond(pedidos)
                            } else {
                                call.respond(HttpStatusCode.Forbidden)
                            }
                        }
                    }
                    get("/{id}") {
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)
                        val idPedido = call.parameters["id"]?.toIntOrNull()

                        if (idPedido == null) {
                            call.respond(HttpStatusCode.BadRequest, "ID de pedido inválido")
                            return@get
                        }

                        val pedidoActual = Pedido.obtenerPedidoPorId(idPedido)  // Asume que tienes una función similar para cargar el pedido desde la DB

                        if (pedidoActual == null) {
                            call.respond(HttpStatusCode.NotFound, "Pedido no encontrado")
                            return@get
                        }

                        val cliente = Cliente.obtenerClientePorUsuario(idUsuario!!)

                        if (Usuario.esAdministrador(idUsuario) || cliente != null && cliente.id.value == pedidoActual.idCliente) {
                            call.respond(HttpStatusCode.OK, pedidoActual)
                        } else {
                            call.respond(HttpStatusCode.Forbidden)
                        }
                    }

                    get("/enProceso") {
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)
                        val cliente = Cliente.obtenerClientePorUsuario(idUsuario!!)
                        if (cliente != null) {
                            val pedidoDto = Pedido.pedidoEnProceso(cliente.id.value)
                            if (pedidoDto != null) {
                                call.respond(pedidoDto)
                            } else {
                                call.respond(HttpStatusCode.NotFound, "No hay pedidos en proceso para el cliente.")
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden, "Acceso denegado.")
                        }
                    }
                    get("/pedidosCliente") {
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)

                        if (idUsuario == null) {
                            call.respond(HttpStatusCode.Unauthorized, "Token inválido o no proporcionado")
                            return@get
                        }

                        if (Usuario.esAdministrador(idUsuario)) {

                            call.respond(HttpStatusCode.Forbidden, "Operación no permitida para administradores")
                        } else {
                            val cliente = Cliente.obtenerClientePorUsuario(idUsuario)
                            if (cliente != null) {
                                val pedidos = Pedido.obtenerPedidosPorCliente(cliente.id.value)
                                call.respond(pedidos)
                                println("Respuesta completa del servidor: $pedidos")
                            } else {
                                call.respond(HttpStatusCode.NotFound, "Cliente no encontrado")
                            }
                        }
                    }


                    put("/{id}") {
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)
                        val pedidoDto = call.receive<Pedido.Companion.PedidoDto>()
                        val idPedido = call.parameters["id"]?.toIntOrNull()

                        val pedidoActual = Pedido.obtenerPedidoPorId(idPedido!!) // Supongo que tienes una función similar para cargar el pedido desde la DB

                        val cliente = Cliente.obtenerClientePorUsuario(idUsuario!!)

                        if (pedidoActual == null) {
                            call.respond(HttpStatusCode.NotFound, "Pedido no encontrado")
                            return@put
                        }

                        if (Usuario.esAdministrador(idUsuario) || cliente!!.id.value == pedidoDto.idCliente || cliente!!.id.value == pedidoActual.idCliente) {
                            val success = Pedido.actualizarPedido(
                                idPedido!!,
                                Pedidos.EstadoPedido.valueOf(pedidoDto.estado)
                            )
                            if (success) {
                                call.respond(HttpStatusCode.OK)
                            } else {
                                call.respond(HttpStatusCode.BadRequest, "No se pudo actualizar el pedido")
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden)
                        }
                    }


                    delete("/{id}") {
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)
                        val idPedido = call.parameters["id"]?.toIntOrNull()
                        if (idPedido == null) {
                            call.respond(HttpStatusCode.BadRequest, "ID de pedido inválido")
                            return@delete
                        }

                        val cliente = Cliente.obtenerClientePorUsuario(idUsuario!!)
                        val pedido = Pedido.findById(idPedido)  // Suponiendo que findById es una función válida.

                        if (cliente != null && (Usuario.esAdministrador(idUsuario) || cliente.id.value == pedido?.id_cliente?.value)) {
                            val success = Pedido.borrarPedido(idPedido)
                            if (success) {
                                call.respond(HttpStatusCode.NoContent)
                            } else {
                                call.respond(HttpStatusCode.BadRequest, "No se pudo eliminar el pedido")
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden)
                        }
                    }
                }
                route("/productos") {
                    post {
                        Conexion.conectar()
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)
                        println("prueba2" + idUsuario)
                        if (Usuario.esAdministrador(idUsuario!!)) {
                            val productoDto = call.receive<Producto.Companion.ProductoDto>()
                            val success = crearProducto( // Asume que esta función devuelve un valor Boolean
                                productoDto.nombre,
                                productoDto.descripcion,
                                productoDto.precio,
                                productoDto.idCategoria,
                                productoDto.imagen // Mantenemos el nombre original de la imagen
                            )
                            if (success) {
                                call.respond(HttpStatusCode.Created)
                            } else {
                                call.respond(HttpStatusCode.BadRequest, "No se pudo crear el producto")
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden)
                        }
                    }






                    delete("/{id}") {
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)
                        if (Usuario.esAdministrador(idUsuario!!)) {
                            val id = call.parameters["id"]?.toIntOrNull()
                            val success = id?.let { Producto.borrarProducto(it) }
                            if (success == true) {
                                call.respond(HttpStatusCode.NoContent)
                            } else {
                                call.respond(HttpStatusCode.NotFound)
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden)
                        }
                    }

                    put("/{id}") {
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)
                        if (Usuario.esAdministrador(idUsuario!!)) {
                            val productoDto = call.receive<Producto.Companion.ProductoDto>()
                            val id = call.parameters["id"]?.toIntOrNull()

                            // Obtenemos el producto antiguo para borrar su imagen
                            val oldProducto = Producto.obtenerProductoPorId(id!!)
                            if (oldProducto != null) {
                                val oldImageName = oldProducto.imagen
                                val oldImagePath = Paths.get("resources/img_productos/$oldImageName")

                                // Comprobamos si la imagen antigua existe antes de intentar borrarla
                                if (Files.exists(oldImagePath)) {
                                    Files.deleteIfExists(oldImagePath)
                                }
                            }

                            val success = id?.let {
                                Producto.actualizarProducto(
                                    it,
                                    productoDto.nombre,
                                    productoDto.descripcion,
                                    productoDto.precio,
                                    productoDto.idCategoria,
                                    productoDto.imagen
                                )
                            }

                            if (success == true) {
                                call.respond(HttpStatusCode.OK)
                            } else {
                                call.respond(HttpStatusCode.NotFound)
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden)
                        }
                    }

                    get {
                        val productos = Producto.obtenerProductos()
                        call.respond(productos)
                    }
                    get("/{id}") {
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)

                            val id = call.parameters["id"]?.toIntOrNull()
                            val productoDto = id?.let { Producto.obtenerProductoPorId(it) }
                            if (productoDto != null) {
                                call.respond(HttpStatusCode.OK, productoDto)
                            } else {
                                call.respond(HttpStatusCode.NotFound, "Producto no encontrado")
                            }
                        }
                }
                route ("/valoraciones") {
                    post {
                        val valoracionDto = call.receive<Valoracion.Companion.ValoracionDto>()
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)
                        if (idUsuario != null) {
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
                        } else {
                            call.respond(HttpStatusCode.Forbidden)
                        }
                    }

                    delete("/{id}") {
                        val id = call.parameters["id"]?.toIntOrNull()
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)
                        if (id != null && idUsuario != null) {
                            val valoracion = Valoracion.findById(id)
                            if (valoracion != null && (idUsuario == valoracion.id_cliente.id_usuario.id.value || Usuario.esAdministrador(idUsuario))) {
                                val success = Valoracion.borrarValoracion(id)
                                if (success) {
                                    call.respond(HttpStatusCode.NoContent)
                                } else {
                                    call.respond(HttpStatusCode.NotFound)
                                }
                            } else {
                                call.respond(HttpStatusCode.Forbidden)
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden)
                        }
                    }

                    put("/{id}") {
                        val valoracionDto = call.receive<Valoracion.Companion.ValoracionDto>()
                        val id = call.parameters["id"]?.toIntOrNull()
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)
                        if (id != null && idUsuario != null) {
                            val valoracion = Valoracion.findById(id)
                            if (valoracion != null && (idUsuario == valoracion.id_cliente.id_usuario.id.value || Usuario.esAdministrador(idUsuario))) {
                                val success = Valoracion.actualizarValoracion(id, valoracionDto.puntuacion, valoracionDto.comentario)
                                if (success) {
                                    call.respond(HttpStatusCode.OK)
                                } else {
                                    call.respond(HttpStatusCode.NotFound)
                                }
                            } else {
                                call.respond(HttpStatusCode.Forbidden)
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden)
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

                    get("/valoracionesProducto/{idProducto}") {
                        val idProducto = call.parameters["idProducto"]?.toIntOrNull()
                        if (idProducto != null) {
                            val valoraciones = Valoracion.obtenerValoracionesPorProducto(idProducto)
                            if (valoraciones.isNotEmpty()) {
                                call.respond(HttpStatusCode.OK, valoraciones)
                            } else {
                                call.respond(HttpStatusCode.NotFound, "No se encontraron valoraciones para el producto especificado")
                            }
                        } else {
                            call.respond(HttpStatusCode.BadRequest, "ID de producto inválido")
                        }
                    }
                }
                route("/usuarios") {
                    post("/crear") {
                        val usuarioDto = call.receive<Usuario.Companion.UsuarioDto>()
                        val resultado = Usuario.crearUsuario(usuarioDto)
                        if (resultado) {
                            call.respond(HttpStatusCode.Created, "Usuario creado correctamente")
                        } else {
                            call.respond(HttpStatusCode.BadRequest, "No se pudo crear el usuario")
                        }
                    }
                    get("/tipo/{id}") {
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)
                        val id = call.parameters["id"]?.toIntOrNull()

                        if (id != null && idUsuario != null && (idUsuario == id || Usuario.esAdministrador(idUsuario))) {
                            val tipo = Usuario.tipoDeUsuario(id)
                            call.respond(HttpStatusCode.OK, "El usuario con ID $id es un: $tipo")
                        } else {
                            call.respond(HttpStatusCode.Forbidden, "No tienes permiso para acceder a esta información.")
                        }
                    }


                    delete("/eliminar/{id}") {
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)
                        val id = call.parameters["id"]?.toIntOrNull()
                        if (id != null && idUsuario != null && (idUsuario == id || Usuario.esAdministrador(idUsuario))) {
                            val resultado = Usuario.borrarUsuario(id)
                            if (resultado) {
                                call.respond(HttpStatusCode.OK, "Usuario eliminado correctamente")
                            } else {
                                call.respond(HttpStatusCode.NotFound, "No se encontró el usuario con ID: $id")
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden)
                        }
                    }

                    put("/actualizar") {
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)

                        if (idUsuario != null || Usuario.esAdministrador(idUsuario!!)) {
                            val usuarioDto = call.receive<Usuario.Companion.UsuarioDto>()
                            usuarioDto.id = idUsuario
                            val resultado = Usuario.actualizarUsuario(usuarioDto)
                            if (resultado) {
                                call.respond(HttpStatusCode.OK, "Usuario actualizado correctamente")
                            } else {
                                call.respond(HttpStatusCode.NotFound, "No se encontró el usuario con ID: $idUsuario")
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden)
                        }
                    }
                    get("/obtener") {
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)
                        if (idUsuario != null) {
                            val usuario = Usuario.obtenerUsuarioPorId(idUsuario)
                            if (usuario != null) {
                                call.respond(HttpStatusCode.OK, usuario)
                            } else {
                                call.respond(HttpStatusCode.NotFound, "No se encontró el usuario con ID: $idUsuario")
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden)
                        }
                    }
                    get("/todos") {
                        val idUsuario = obtenerIdUsuarioDesdeToken(call)
                        if (idUsuario != null && Usuario.esAdministrador(idUsuario)) {
                            val todosLosUsuarios = Usuario.obtenerTodosLosUsuarios()
                            if (todosLosUsuarios.isNotEmpty()) {
                                call.respond(HttpStatusCode.OK, todosLosUsuarios)
                            } else {
                                call.respond(HttpStatusCode.NotFound, "No hay usuarios en la base de datos.")
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden, "No tienes permiso para acceder a esta información.")
                        }
                    }
                    post("/deshabilitar/admin/{id}") {
                        val idUsuario = obtenerIdUsuarioDesdeToken(call) // Suponiendo que tienes un método para obtener el ID del usuario desde el token
                        val id = call.parameters["id"]?.toIntOrNull()

                        if (id != null && idUsuario != null && Usuario.esAdministrador(idUsuario)) {
                            val resultado = Usuario.deshabilitarCuenta(id)
                            if (resultado) {
                                call.respond(HttpStatusCode.OK, "La cuenta del usuario con ID $id ha sido deshabilitada.")
                            } else {
                                call.respond(HttpStatusCode.NotFound, "No se encontró el usuario con ID $id.")
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden, "No tienes permisos para deshabilitar esta cuenta.")
                        }
                    }

                    // Endpoint que obtiene el ID a partir del token
                    post("/deshabilitar") {
                        val idUsuario = obtenerIdUsuarioDesdeToken(call) // Suponiendo que tienes un método para obtener el ID del usuario desde el token

                        if (idUsuario != null) {
                            val resultado = Usuario.deshabilitarCuenta(idUsuario)
                            if (resultado) {
                                call.respond(HttpStatusCode.OK, "Tu cuenta ha sido deshabilitada.")
                            } else {
                                call.respond(HttpStatusCode.NotFound, "No se pudo encontrar tu cuenta.")
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden, "No tienes permisos para deshabilitar esta cuenta.")
                        }
                    }
                }


                }


            }
        }

    }

suspend fun InputStream.copyToSuspend(out: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
    var bytesCopied: Long = 0
    val buffer = ByteArray(bufferSize)
    var bytes = read(buffer)
    while (bytes >= 0) {
        out.write(buffer, 0, bytes)
        bytesCopied += bytes
        bytes = read(buffer)
    }
    return bytesCopied
}



