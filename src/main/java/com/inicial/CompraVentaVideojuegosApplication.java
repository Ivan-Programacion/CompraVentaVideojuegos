package com.inicial;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

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
				+ "                    vendedor_id BIGINT NOT NULL,\n"
				+ "                    comprador_id BIGINT DEFAULT NULL,\n"
				+ "                    CONSTRAINT fk_vendedor FOREIGN KEY (vendedor_id) REFERENCES usuarios(id) ON DELETE CASCADE,\n"
				+ "                    CONSTRAINT fk_comprador FOREIGN KEY (comprador_id) REFERENCES usuarios(id) ON DELETE SET NULL)");
		jdbcTemplate.update("insert into usuarios (nombre, pwd, saldo, admin) values (?,?,?,?)", nombreTienda, "", 0,
				true);
		return "Se han creado las tablas correctamente";
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
	public List<Juego> juegos() {
		// Listamos todos los juegos
		List<Juego> listaJuegos = jdbcTemplate
				.query("select * from juegos where aceptado = true and comprador_id is NULL", new LisarJuegos());
		return listaJuegos;
	}

	/**
	 * Endpoint para subir un juego a la BBDD
	 */

	// VER @RequestParam para los parámetros. Evitamos problemas de decimales y
	// nombres con espacios
	@GetMapping("/subirjuego/{idVendedor}/{nombre}/{imagen}/{precio}/{clave}")
	public String subirJuego(@PathVariable Long idVendedor, @PathVariable String nombre, @PathVariable String imagen,
			@PathVariable double precio, @PathVariable String clave) {

		List<Usuario> user = jdbcTemplate.query("SELECT * FROM usuarios WHERE id = ?", new ListarUsuarios(),
				idVendedor);
		if (user.isEmpty()) {
			return "El usuario no ha sido encontrado en la BBDD";
		}

		try {
			jdbcTemplate.update(
					"INSERT INTO juegos(nombre, imagen, precio, clave, vendedor_id, aceptado, comprador_id) VALUES (?,?,?,?,?, false, NULL)",
					nombre, imagen, precio, clave, idVendedor);
			return "Se ha subido el juego correctamente :)";
		} catch (Exception e) {
			return "Ha habido un error al subir el juego" + e.getMessage();
		}

	}

	/**
	 * Método para aprobar anuncios desde admin
	 */
	@GetMapping("/admin/aprobarJuego/{idJuego}/{idUsuario}")
	public String aprobarJuego(@PathVariable Long idJuego, @PathVariable Long idUsuario) {
		List<Usuario> usuarios = jdbcTemplate.query("SELECT * FROM usuarios WHERE id = ?", new ListarUsuarios(),
				idUsuario);
		if (usuarios.isEmpty() || !usuarios.get(0).isAdmin()) {
			return "No cuentas con permisos para aprobar anuncios";
		}

		int fila = jdbcTemplate.update("UPDATE juegos SET aceptado = true WHERE id = ?", idJuego);
		if (fila > 0) {
			return "Juego aprobado";
		} else {
			return "Ha ocurrido un error al aprobar el juego";
		}
	}

	/**
	 * Método para rechazar anuncios desde admin
	 */

	@GetMapping("/admin/rechazarJuego/{idJuego}/{idUsuario}")
	public String rechazarJuego(@PathVariable Long idJuego, @PathVariable Long idUsuario) {
		List<Usuario> usuarios = jdbcTemplate.query("SELECT * FROM usuarios WHERE id = ?", new ListarUsuarios(),
				idUsuario);
		if (usuarios.isEmpty() || !usuarios.get(0).isAdmin()) {
			return "No cuentas con permisos para eliminar anuncios";
		}

		int filaBorrada = jdbcTemplate.update("DELETE FROM juegos WHERE id = ?", idJuego);
		if (filaBorrada > 0) {
			return "Se ha borrado el anuncio";
		} else {
			return "Error al borrar el anuncio";

		}
	}

	/**
	 * Endpoint para listar los juegos NO aprobados
	 * 
	 * @return la lista de los juegos NO aprobados
	 */
	@GetMapping("/juegosPendientes")
	public List<Juego> juegosPendientes() {
		// Listamos todos los juegos
		List<Juego> listaJuegos = jdbcTemplate.query("select * from juegos where aceptado != true", new LisarJuegos());
		return listaJuegos;
	}

	/**
	 * Listar juegos de mi biblioteca (comprados)
	 */
	@GetMapping("/misJuegos/{idUsuario}")
	public List<Juego> misJuegos(@PathVariable Long idUsuario) {
		return jdbcTemplate.query("SELECT * FROM juegos WHERE comprador_id =  ?", new LisarJuegos(), idUsuario);

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
