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

	@GetMapping("/registroAdmin/{nombre}/{pwd}")
	public Usuario registroAdmin(@PathVariable String nombre, @PathVariable String pwd) {
		// Guardamos en una lista todos los usuarios que coincidan con el nombre que se
		// intenta registrar
		List<Usuario> listaUsuarios = jdbcTemplate.query("select * from usuarios where nombre = ?",
				new ListarUsuarios(), nombre);
		// Si la lista de usuarios está vacía, significa que no existe ningún usuario
		// con ese nombre
		System.out.println(listaUsuarios.toString()); // PRUEBA ---------------------------------------
		if (listaUsuarios.isEmpty()) {
			// Creamos el usuario y lo devolvemos en el return
			jdbcTemplate.update("insert into usuarios (nombre, pwd, saldo, admin) values (?,?,?,?)", nombre, hashearPwd(pwd), 0,
					true);
			listaUsuarios = jdbcTemplate.query("select * from usuarios where nombre = ?", new ListarUsuarios(), nombre);
			System.out.println(listaUsuarios.toString()); // PRUEBA ---------------------------------------
			Usuario adminNuevo = listaUsuarios.get(0);
			return adminNuevo;
		}
		// Si ya existe el usuario, devolvemos null
		return null;
	}

	@GetMapping("/registro/{nombre}/{pwd}")
	public Usuario registro(@PathVariable String nombre, @PathVariable String pwd) {
		// Guardamos en una lista todos los usuarios que coincidan con el nombre que se
		// intenta registrar
		List<Usuario> listaUsuarios = jdbcTemplate.query("select * from usuarios where nombre = ?",
				new ListarUsuarios(), nombre);
		// Si la lista de usuarios está vacía, significa que no existe ningún usuario
		// con ese nombre
		System.out.println(listaUsuarios.toString()); // PRUEBA ---------------------------------------
		if (listaUsuarios.isEmpty()) {
			// Creamos el usuario y lo devolvemos en el return
			jdbcTemplate.update("insert into usuarios (nombre, pwd, saldo, admin) values (?,?,?,?)", nombre, hashearPwd(pwd), 0,
					false);
			listaUsuarios = jdbcTemplate.query("select * from usuarios where nombre = ?", new ListarUsuarios(), nombre);
			System.out.println(listaUsuarios.toString()); // PRUEBA ---------------------------------------
			Usuario usuarioNuevo = listaUsuarios.get(0);
			return usuarioNuevo;
		}
		// Si ya existe el usuario, devolvemos null
		return null;
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
