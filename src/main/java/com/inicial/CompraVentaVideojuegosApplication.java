package com.inicial;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.PatchExchange;

@SpringBootApplication
@RestController
public class CompraVentaVideojuegosApplication {

	private final JdbcTemplate jdbcTemplate;
	private final String nombreTienda;

	public CompraVentaVideojuegosApplication(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		nombreTienda = "RetroGames";
	}

	public static void main(String[] args) {
		SpringApplication.run(CompraVentaVideojuegosApplication.class, args);
	}

	// Endpoint necesarios:
	// - registro (admin/usuario) ------------> CHECK
	// - login (admin/usuario) ------------> CHECK
	// - datosJuego (datos de juego) ------------> CHECK
	// - subirJuego ------------> CHECK
	// - borrarJuego (admin/usuario) ------------> CHECK
	//
	// USUARIO
	// - listarJuegos (en venta y SIN comprador) ------------> CHECK
	// - buscarJuego (filtro busqueda)
	// - comprarCarrito (comprar lista juegos)
	// - misJuegosComprados ------------> CHECK
	// - misJuegosEnVenta
	// - addSaldo
	//
	// ADMIN
	// - listarJuegosPendientes ------------> CHECK
	// - rechazarJuego ------------> CHECK
	// - listarUsuarios
	// - verUsuario
	// - juegosCompradosPorUsuario (utilizar misJuegosComprados) --> CHECK
	// - juegosEnVentaPorUsuario (utilizar misJuegosEnVenta)

	@GetMapping("/crear")
	public String crearTabla() {
		// Eliminamos si existen
		jdbcTemplate.execute("DROP TABLE IF EXISTS juegos");
		jdbcTemplate.execute("DROP TABLE IF EXISTS usuarios");
		// Creamos las dos tablas
		jdbcTemplate.execute("CREATE TABLE usuarios (\n" + "                    id BIGINT AUTO_INCREMENT PRIMARY KEY,\n"
				+ "                    nombre VARCHAR(100) NOT NULL UNIQUE,\n"
				+ "                    pwd VARCHAR(255) NOT NULL,\n"
				+ "                    saldo DECIMAL(10, 2) DEFAULT 0.00,\n"
				+ "                    admin BOOLEAN DEFAULT FALSE)");
		jdbcTemplate.execute("CREATE TABLE juegos (\n" + "                    id BIGINT AUTO_INCREMENT PRIMARY KEY,\n"
				+ "                    nombre VARCHAR(150) NOT NULL,\n" + "                    imagen VARCHAR(255),\n"
				+ "                    precio DECIMAL(10, 2) NOT NULL,\n"
				+ "                    clave VARCHAR(255) NOT NULL UNIQUE,\n"
				+ "                    aceptado BOOLEAN DEFAULT FALSE,\n"
				+ "                    revisado BOOLEAN DEFAULT FALSE,\n"
				+ "                    vendedor_id BIGINT NOT NULL,\n"
				+ "                    comprador_id BIGINT DEFAULT NULL,\n"
				+ "                    CONSTRAINT fk_vendedor FOREIGN KEY (vendedor_id) REFERENCES usuarios(id) ON DELETE CASCADE,\n"
				+ "                    CONSTRAINT fk_comprador FOREIGN KEY (comprador_id) REFERENCES usuarios(id) ON DELETE SET NULL)");
		jdbcTemplate.update("insert into usuarios (nombre, pwd, saldo, admin) values (?,?,?,?)", nombreTienda, "", 0,
				true);
		return "Se han creado las tablas correctamente";
	}

	/**
	 * Endpoint que accede a los datos de un juego
	 * 
	 * @param idJuego
	 * @return La instancia del juego si se ha encontrado, o nulo en caso de que no
	 *         se haya encontrado
	 */
	@GetMapping("/datosJuego/{idJuego}")
	@CrossOrigin(origins = "*") // Para que se pueda leer en web (HTML)
	public Juego datosJuego(@PathVariable Long idJuego) {
		Juego juego = null;
		List<Juego> juegos = jdbcTemplate.query("select * from juegos where id = ?", new ListarJuegos(), idJuego);
		if (juegos.isEmpty())
			return null;
		juego = juegos.get(0);
		return juego;
	}

	/**
	 * Endpoint que borra un juego
	 * 
	 * @param idJuego    ID del juego
	 * @param idVendedor ID del vendedor del juego
	 * @return
	 */
	@GetMapping("/borrarJuego")
	public boolean borrarJuego(@PathVariable Long idJuego, @PathVariable Long idVendedor) {
		List<Juego> juegos = jdbcTemplate.query("select * from juegos where id = ? and vendedor_id = ?",
				new ListarJuegos(), idJuego, idVendedor);
		List<Usuario> usuarios = jdbcTemplate.query("select * from usuarios where id = ?", new ListarUsuarios(),
				idVendedor);
		if (juegos.isEmpty() || usuarios.isEmpty()) {
			System.err.println("USUARIO " + idVendedor + " intentó eliminar juego --> ERROR");
			return false;
		}
		Usuario usuario = usuarios.get(0);
		jdbcTemplate.update("DELETE from juegos where id = ?", idJuego);
		System.out.println("USUARIO " + usuario.getNombre() + " ha eliminado un juego --> ID: " + idJuego);
		return true;
	}

	/**
	 * Endpoint para registro de admin
	 * 
	 * @param nombre
	 * @param pwd
	 * @return La instancia nueva del usuario, o nulo si ay existe un usuario con
	 *         ese nombre
	 */
	@GetMapping("/registroAdmin/{nombre}/{pwd}")
	public Usuario registroAdmin(@PathVariable String nombre, @PathVariable String pwd) {
		// Guardamos en una lista todos los usuarios que coincidan con el nombre que se
		// intenta registrar
		List<Usuario> listaUsuarios = jdbcTemplate.query("select * from usuarios where nombre = ?",
				new ListarUsuarios(), nombre);
		// Si la lista de usuarios está vacía, significa que no existe ningún usuario
		// con ese nombre
		if (listaUsuarios.isEmpty()) {
			// Creamos el usuario y lo devolvemos en el return
			jdbcTemplate.update("insert into usuarios (nombre, pwd, saldo, admin) values (?,?,?,?)", nombre,
					hashearPwd(pwd), 0, true);
			listaUsuarios = jdbcTemplate.query("select * from usuarios where nombre = ?", new ListarUsuarios(), nombre);
			System.out.println("ADMIN REGISTRADO " + listaUsuarios.toString()); // LOG
			Usuario adminNuevo = listaUsuarios.get(0);
			return adminNuevo;
		}
		// Si ya existe el usuario, devolvemos null
		return null;
	}

	/**
	 * Endpoint para registro usuario
	 * 
	 * @param nombre
	 * @param pwd
	 * @return La instancia nueva del usuario, o nulo si ay existe un usuario con
	 *         ese nombre
	 */
	@GetMapping("/registro/{nombre}/{pwd}")
	@CrossOrigin(origins = "*") // Para que se pueda leer en web (HTML)
	public Usuario registro(@PathVariable String nombre, @PathVariable String pwd) {
		// Guardamos en una lista todos los usuarios que coincidan con el nombre que se
		// intenta registrar
		List<Usuario> listaUsuarios = jdbcTemplate.query("select * from usuarios where nombre = ?",
				new ListarUsuarios(), nombre);
		// Si la lista de usuarios está vacía, significa que no existe ningún usuario
		// con ese nombre
		if (listaUsuarios.isEmpty()) {
			// Creamos el usuario y lo devolvemos en el return
			jdbcTemplate.update("insert into usuarios (nombre, pwd, saldo, admin) values (?,?,?,?)", nombre,
					hashearPwd(pwd), 0, false);
			listaUsuarios = jdbcTemplate.query("select * from usuarios where nombre = ?", new ListarUsuarios(), nombre);
			System.out.println("REGISTRO NUEVO: " + listaUsuarios.toString()); // LOG
			Usuario usuarioNuevo = listaUsuarios.get(0);
			return usuarioNuevo;
		}
		// Si ya existe el usuario, devolvemos null
		return null;
	}

	/**
	 * Endpoint para realizar el login
	 * 
	 * @param nombre
	 * @param pwd
	 * @return El usuario si existe. Si no existe, nulo
	 */
	@GetMapping("/login/{nombre}/{pwd}")
	@CrossOrigin(origins = "*") // Para que se pueda leer en web (HTML)
	public Usuario login(@PathVariable String nombre, @PathVariable String pwd) {
		// Guardamos en una lista todos los usuarios que coincidan con el nombre que se
		// intenta registrar
		List<Usuario> listaUsuarios = jdbcTemplate.query("select * from usuarios where nombre = ? and pwd = ?",
				new ListarUsuarios(), nombre, hashearPwd(pwd));
		// Si la lista de usuarios NO está vacía, significa que ya tiene cuenta
		if (!listaUsuarios.isEmpty()) {
			System.out.println("LOGIN USARIO: " + listaUsuarios.toString()); // LOG
			Usuario loginUsuario = listaUsuarios.get(0);
			// Se le pasa su usario
			return loginUsuario;
		}
		System.err.println("INTENTO DE LOGIN FALLIDO: " + nombre); // LOG
		// Si la lista está vacía significa que, o el usuario no coincide o la
		// contraseña está mal. Y se devuelve null
		return null;
	}

	/**
	 * Endpoint para listar los juegos APROBADOS y los que NO tienen comprador aun
	 * 
	 * @return La lista de los juegos APROBADOS y que no han sido comprados
	 */
	@GetMapping("/juegos")
	@CrossOrigin(origins = "*") // Para que se pueda leer en web (HTML)
	public List<Juego> juegos() {
		// Listamos todos los juegos
		List<Juego> listaJuegos = jdbcTemplate
				.query("select * from juegos where aceptado = true and comprador_id is NULL", new ListarJuegos());
		return listaJuegos;
	}

	/**
	 * Endpoint para subir un juego a la BBDD.
	 * 
	 * @param idVendedor
	 * @param nombre
	 * @param imagen
	 * @param precio
	 * @param clave
	 * @return Devuelve true si se ha podido realizar o false si no se ha realizado
	 */
	// VER @RequestParam para los parámetros. Evitamos problemas de decimales y
	// nombres con espacios
	@GetMapping("/subirJuego/{idVendedor}/{nombre}/{imagen}/{precio}/{clave}")
	@CrossOrigin(origins = "*") // Para que se pueda leer en web (HTML)
	public boolean subirJuego(@PathVariable Long idVendedor, @PathVariable String nombre, @PathVariable String imagen,
			@PathVariable double precio, @PathVariable String clave) {

		List<Usuario> user = jdbcTemplate.query("SELECT * FROM usuarios WHERE id = ?", new ListarUsuarios(),
				idVendedor);
		if (user.isEmpty()) {
			System.err
					.println("USUARIO " + idVendedor + "intentó subir un juego --> ERROR: no se ha encontrado usuario");
			return false; // El usuario no ha sido encontrado en la BBDD
		}
		Usuario usuario = user.get(0);
		boolean admin = true;
		String nombreFinal = "";
		// Si es admin, añadimos el nombre de la tienda y en el insert añadimos
		// "aceptado" a true con la variable admin
		if (usuario.isAdmin()) {
			nombreFinal = usuario.getNombre();
		} else {
			nombreFinal = nombre;
			admin = false;
		}
		try {
			jdbcTemplate.update(
					"INSERT INTO juegos(nombre, imagen, precio, clave, vendedor_id, aceptado, comprador_id) VALUES (?,?,?,?,?,?,?, NULL)",
					nombreFinal, imagen, precio, clave, idVendedor, admin, admin); // Si es admin devuelve true, si es
																					// usuario devuelve false
			System.out.println("USUARIO " + idVendedor + " ha subido un juego");
			return true; // Se ha subido el juego correctamente :
		} catch (Exception e) {
			System.err.println("USUARIO " + idVendedor + "intentó subir un juego --> ERROR: " + e.getMessage());
			return false;
		}

	}

	/**
	 * Endpoint que aprueba un juego desde admin
	 * 
	 * @param idJuego
	 * @param idUsuario
	 * @return Devuelve true si se ha realizado correctamente, o false si no se ha
	 *         podido realizar
	 */
	@GetMapping("/admin/aprobarJuego/{idJuego}/{idUsuario}")
	public boolean aprobarJuego(@PathVariable Long idJuego, @PathVariable Long idUsuario) {
		List<Usuario> usuarios = jdbcTemplate.query("SELECT * FROM usuarios WHERE id = ?", new ListarUsuarios(),
				idUsuario);
		if (usuarios.isEmpty() || !usuarios.get(0).isAdmin()) {
			System.err.println("USUARIO " + idUsuario + "intentó aprobar un juego --> ERROR: NO ES ADMIN");
			return false; // No cuentas con permisos para aprobar anuncios
		}
		List<Juego> juegos = jdbcTemplate.query("SELECT * FROM juegos WHERE id = ?", new ListarJuegos(), idJuego);
		Juego juego = juegos.get(0);
		if (!juego.isRevisado()) {
			int fila = jdbcTemplate.update("UPDATE juegos SET aceptado = true, revisado = true WHERE id = ?", idJuego);
			if (fila > 0) {
				System.out.println("Juego aprobado por admin --> ID JUEGO: " + idJuego);
				return true; // Juego aprobado
			} else {
				System.err.println("Intento de subir juego por admin -->ERROR: ID JUEGO: " + idJuego);
				return false; // Ha ocurrido un error al aprobar el juego
			}
		}
		return false;
	}

	/**
	 * Endpoint que realizara la acción de rechazar un juego desde admin
	 * 
	 * @param idJuego
	 * @param idUsuario
	 * @return Devuelve true si lo ha realizado correctamente, o false en caos de
	 *         que haya fallado
	 */

	@GetMapping("/admin/rechazarJuego/{idJuego}/{idUsuario}")
	public boolean rechazarJuego(@PathVariable Long idJuego, @PathVariable Long idUsuario) {
		List<Usuario> usuarios = jdbcTemplate.query("SELECT * FROM usuarios WHERE id = ?", new ListarUsuarios(),
				idUsuario);
		if (usuarios.isEmpty() || !usuarios.get(0).isAdmin()) {
			System.err.println("USUARIO " + idUsuario + "intentó rechazar un juego --> ERROR: NO ES ADMIN");
			return false; // No cuentas con permisos para aprobar anuncios
		}
		List<Juego> juegos = jdbcTemplate.query("SELECT * FROM juegos WHERE id = ?", new ListarJuegos(), idJuego);
		Juego juego = juegos.get(0);
		if (!juego.isRevisado()) {
			int fila = jdbcTemplate.update("UPDATE juegos SET aceptado = false, revisado = true WHERE id = ?", idJuego);
			if (fila > 0) {
				System.out.println("Juego rechazado por admin --> ID JUEGO: " + idJuego);
				return true; // Se ha borrado el anuncio
			} else {
				System.err.println("Intento de subir juego por admin -->ERROR: ID JUEGO: " + idJuego);
				return false; // Error al borrar el anuncio
			}
		}
		return false;
	}

	/**
	 * Endpoint para listar los juegos NO aprobados
	 * 
	 * @return la lista de los juegos NO aprobados
	 */
	@GetMapping("/juegosPendientes")
	public List<Juego> juegosPendientes() {
		// Listamos todos los juegos
		List<Juego> listaJuegos = jdbcTemplate.query("select * from juegos where revisado = false", new ListarJuegos());
		return listaJuegos;
	}

	/**
	 * Listar juegos de mi biblioteca (comprados)
	 */
	@GetMapping("/misJuegos/{idUsuario}")
	@CrossOrigin(origins = "*") // Para que se pueda leer en web (HTML)
	public List<Juego> misJuegos(@PathVariable Long idUsuario) {
		return jdbcTemplate.query("SELECT * FROM juegos WHERE comprador_id =  ?", new ListarJuegos(), idUsuario);

	}

	// ===================== MÉTODOS NO MAPPEADOS ===================== //

	private String hashearPwd(String pwd) {
		String HashedPwd = "";
		try {
			// Cogemos la instancia (utilizaremos el método de hasheo MD5)
			MessageDigest md = MessageDigest.getInstance("SHA3-256");
			// Convertimos la contraseña en bytes y actualizamos el contenido en la
			// instancia
			md.update(pwd.getBytes());
			// Se realiza el resumen de la contraseña y se guarda en un String
			HashedPwd = new String(md.digest());

		} catch (NoSuchAlgorithmException e) {
			System.err.println("Error al hashear la contraseña");
			// e.printStackTrace();
		}
		return HashedPwd;
	}

}
