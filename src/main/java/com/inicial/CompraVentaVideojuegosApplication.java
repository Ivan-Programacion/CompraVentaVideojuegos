package com.inicial;

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

	public CompraVentaVideojuegosApplication(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
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
		return "Se han creado las tablas correctamente";
	}

	@GetMapping("/registroAdmin/{nombre}/{pwd}")
	public boolean registroAdmin(@PathVariable String nombre, @PathVariable String pwd) {
		// Guardamos en una lista todos los usuarios que coincidan con el nombre que se
		// intenta registrar
		List<Usuario> listaUsuarios = jdbcTemplate
				.query(String.format("select * from usuarios where nombre = %s", nombre), new ListarUsuarios());
		// Si la lista de usuarios está vacía, significa que no existe ningún usuario
		// con ese nombre
		if (listaUsuarios.isEmpty())
			return true;
		return false;
	}
	
	@GetMapping("/registro/{nombre}/{pwd}")
	public boolean registro(@PathVariable String nombre, @PathVariable String pwd) {
		// Guardamos en una lista todos los usuarios que coincidan con el nombre que se
		// intenta registrar
		List<Usuario> listaUsuarios = jdbcTemplate
				.query(String.format("select * from usuarios where nombre = %s", nombre), new ListarUsuarios());
		// Si la lista de usuarios está vacía, significa que no existe ningún usuario
		// con ese nombre
		if (listaUsuarios.isEmpty())
			return true;
		return false;
	}

}
