package com.inicial;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class CompraVentaVideojuegosApplication {

	private final JdbcTemplate jdbcTemplate;
	private final Long ID_TIENDA;
	private final String NOMBRE_TIENDA;
	private final BigDecimal COMISION;
	private final BigDecimal COMISION_INVERSA;

	public CompraVentaVideojuegosApplication(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		ID_TIENDA = 1L;
		NOMBRE_TIENDA = "RetroGames";
		// Comision general --> 10% (0.1)
		COMISION = new BigDecimal("0.1");
		// Comisión inversa (para sacar la comisión de un precio final)
		// Habria que hacer una regla de tres sobre el precio del juego:
		//
		// precio ---------> 110% (100% por el vendedor + 10% comisión)
		// X --------------> 10% (X será el 10% del precio original)
		//
		// Que se resume en: precio * 10 / 110
		// Lo mismo que: precio * 1 / 11
		// Que se queda en: precio / 11 --> De ahí sale el 11
		COMISION_INVERSA = new BigDecimal("11");

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
	// - comprarCarrito (comprar lista juegos) implementar clave
	// - misJuegosComprados ------------> CHECK
	// - misJuegosEnVenta ------------> CHECK
	// - addSaldo ------------> CHECK
	// - verUsuario ------------> CHECK
	// - buscarJuego (filtro busqueda) ------------> CHECK
	// - misJuegosComprados ------------> CHECK
	//
	// ADMIN
	// - aprobarJuego ------------> CHECK
	// - listarJuegosPendientes ------------> CHECK
	// - rechazarJuego ------------> CHECK
	//
	// ===== OPCIONALES =====
	// ADMIN
	// - listarUsuarios
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
		jdbcTemplate.update("insert into usuarios (nombre, pwd, saldo, admin) values (?,?,?,?)", NOMBRE_TIENDA, "", 0,
				true);
		return "Se han creado las tablas correctamente";
	}

	/**
	 * Endpoint que realiza la acción de compra del usuario de todo el carrito
	 * 
	 * @param idComprador
	 * @param juegos
	 * @return devuelve un JSON con los ID de los juegos y su clave para canjearlo,
	 *         o nulo en caso de que haya ocurrido algún error
	 */
	@GetMapping("/comprarCarrito/{idComprador}/{juegos}")
	@Transactional // --> Para que cumpla con los requisitos de Transacción y no haya errores en
					// Base de Datos
	@CrossOrigin(origins = "*") // Para que se pueda leer en web (HTML)
	public HashMap<Long, String> comprarCarrito(@PathVariable Long idComprador, @PathVariable List<Long> juegos) {
		try {
			// Miramos si el usuario existe
			List<Usuario> usuarios = jdbcTemplate.query("select * from usuarios where id = ?", new ListarUsuarios(),
					idComprador);
			if (usuarios.isEmpty())
				return null;
			Usuario usuario = usuarios.get(0);
			String query = "select * from juegos where id = ?";
			ArrayList<Juego> listaJuegos = new ArrayList<>();
			// Añadimos los juegos a la lista, si existen o si están disponibles.
			for (Long id : juegos) {
				List<Juego> juego = jdbcTemplate.query(query, new ListarJuegos(), id);
				if (juego.isEmpty()) {
					System.err.println(
							"USUARIO " + idComprador + " intentó comprar carrito --> ERROR: no se encontró juego");
					return null;
				}
				listaJuegos.add(juego.get(0));
			}
			// HASH MAP donde añadiremos las claves
			HashMap<Long, String> claves = new HashMap<>();
			for (Juego juego : listaJuegos) {
				if (juego.getVendedor_id() == idComprador) {
					System.err.println(
							"USUARIO " + idComprador + " intentó comprar juegos --> ERROR: comprando un juego propio");
					return null;
				}
				claves.put(juego.getId(), juego.getClave());
			}

			// Sumamos el total que tiene que pagar el usuario
			// Inicializamos a 0
			BigDecimal totalPagar = BigDecimal.ZERO;
			for (Juego juego : listaJuegos)
				totalPagar = totalPagar.add(juego.getPrecio());
			System.out.println("TOTAL A PAGAR POR USUARIO " + idComprador + " --> " + totalPagar + "€");
			// usuario.getSaldo < totalPagar
			if (usuario.getSaldo().compareTo(totalPagar) < 0) {
				System.err.println("USUARIO " + idComprador + " intentó pagar juegos --> ERROR: saldo insuficiente");
				return null;
			}
			// Le quitamos lo que ha pagado
			usuario.setSaldo(usuario.getSaldo().subtract(totalPagar));

			// buscamos los usuarios que han vendido los juegos
			// Lo guardaremos en un HashMap para que, en caso de que el vendedor sea el
			// mismo, no hagamos usuarios duplicados
			HashMap<Long, Usuario> usuariosVendedores = new HashMap<>();
			for (Juego juego : listaJuegos) {
				usuarios = jdbcTemplate.query("select * from usuarios where id = ?", new ListarUsuarios(),
						juego.getVendedor_id());
				if (usuarios.isEmpty())
					return null;
				usuariosVendedores.put(usuarios.get(0).getId(), usuarios.get(0));
			}

			// Tenemos:
			// usuariosVendedores
			// listaJuegos
			// Hay que hacer los cambios primero en local por si hubiera algún fallo en la
			// base de datos
			BigDecimal saldoTienda = BigDecimal.ZERO;
			for (Juego juego : listaJuegos) {
				// Cambiamos el id del comprador
				juego.setComprador_id(idComprador);
				// Recorremos, por cada juego, los diferentes usuarios
				for (Map.Entry<Long, Usuario> entry : usuariosVendedores.entrySet()) {
					// Si el id del vendedor del juego coincide con el usuario, realizamos acción
					if (juego.getVendedor_id() == entry.getKey()) {
						Usuario user = entry.getValue();
						// Si es admin, le damos íntegro todo el dinero que costaba el juego
						if (user.isAdmin())
							user.setSaldo(user.getSaldo().add(juego.getPrecio()));
						// Si no, le daremos el dinero menos la comisión (que se llevará la tienda)
						else {
							// Necesitamos sacar la comisión del precio con --> COMISION_INVERSA
							BigDecimal comisionJuego = juego.getPrecio().divide(COMISION_INVERSA);
							saldoTienda = saldoTienda.add(comisionJuego);
							BigDecimal saldoUser = juego.getPrecio().subtract(comisionJuego);
							user.setSaldo(user.getSaldo().add(saldoUser));
						}
					}
				}
			}
			// Ahora toca actualizar todos los datos de los usuarios y de los juegos
			// 1. Actualizar saldo del comprador
			int fila1 = jdbcTemplate.update("update usuarios set saldo = ? where id = ?", usuario.getSaldo(),
					usuario.getId());
			if (fila1 <= 0)
				// Si falla, debemos lanzar una excepcion para que se haga el rollback
				// automáticamente y no haga los updates anteriores que se hayan hecho.
				throw new Exception();
			// 2. Actualizar saldo vendedores
			for (Juego juego : listaJuegos) {
				int fila2 = jdbcTemplate.update("update juegos set precio = ?, comprador_id = ? where id = ?",
						juego.getPrecio(), juego.getComprador_id(), juego.getId());
				if (fila2 <= 0)
					throw new Exception();
			}
			// 3. Actualizar saldo tienda
			for (Map.Entry<Long, Usuario> entry : usuariosVendedores.entrySet()) {
				int fila = jdbcTemplate.update("update usuarios set saldo = ? where id = ?",
						entry.getValue().getSaldo(), entry.getKey());
				if (fila <= 0)
					throw new Exception();
			}
			// Y por último, toca añadir el saldo correspondiente de las comisiones a la
			// tienda
			int fila = jdbcTemplate.update("update usuarios set saldo = saldo + ? where id = ?", saldoTienda,
					ID_TIENDA);
			if (fila <= 0)
				throw new Exception();
			System.out.println("USUARIO " + usuario.getId() + " - " + usuario.getNombre() + " --> ha comprado:\n"
					+ listaJuegos.toString());
			return claves;
		} catch (Exception e) {
			System.err.println("USUARIO " + idComprador + " intentó comprar carrito --> ERROR: persistencia de datos");
			return null;
		}
	}

	/**
	 * Endpoint que añade saldo a la cuenta
	 * 
	 * @param idUsuario
	 * @param saldo
	 * @return Devuelve true si lo ha añadido correctamente, o false si ha habido
	 *         algún problema y no lo ha actualizado
	 */
	@GetMapping("/addSaldo/{idUsuario}/{saldo}")
	@CrossOrigin(origins = "*") // Para que se pueda leer en web (HTML)
	public boolean addSaldo(@PathVariable Long idUsuario, @PathVariable BigDecimal saldo) {
		List<Usuario> usuarios = jdbcTemplate.query("select * from usuarios where id = ?", new ListarUsuarios(),
				idUsuario);
		if (usuarios.isEmpty())
			return false;
		Usuario usuario = usuarios.get(0);
		// Si el saldo es menor o igual a cero
		if (saldo.compareTo(BigDecimal.ZERO) <= 0) {
			System.err.println("USUARIO " + idUsuario + "intentó añadir saldo --> ERROR: no se ha encontrado usuario");
			return false;
		}
		// saldoUsuario += saldo
		usuario.setSaldo(usuario.getSaldo().add(saldo));
		int fila = jdbcTemplate.update("update usuarios set saldo = ? where id = ?", usuario.getSaldo(),
				usuario.getId());
		if (fila > 0) {
			System.out.println("USUARIO " + idUsuario + " ha añadido saldo --> " + saldo + "€");
			return true;
		} else {
			System.err.println(
					"USUARIO " + idUsuario + "intentó añadir saldo --> ERROR: no se ha actualizado correctamente");
			return false;
		}
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
	 * @return Devuelve true si se ha borrado correctamente, o false si ha habido
	 *         algún problema y no lo ha borrado
	 */
	@GetMapping("/borrarJuego/{idJuego}/{idVendedor}")
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
			try {
				// Creamos el usuario y lo devolvemos en el return
				jdbcTemplate.update("insert into usuarios (nombre, pwd, saldo, admin) values (?,?,?,?)", nombre,
						hashearPwd(pwd), 0, true);
				listaUsuarios = jdbcTemplate.query("select * from usuarios where nombre = ?", new ListarUsuarios(),
						nombre);
				System.out.println("ADMIN REGISTRADO " + listaUsuarios.toString()); // LOG
				Usuario adminNuevo = listaUsuarios.get(0);
				return adminNuevo;
			} catch (Exception e) {
				System.out.println("REGISTRO fallido");
				return null;
			}
		}
		// Si ya existe el usuario, devolvemos null
		System.out.println("REGISTRO fallido");
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
			try {
				// Creamos el usuario y lo devolvemos en el return
				jdbcTemplate.update("insert into usuarios (nombre, pwd, saldo, admin) values (?,?,?,?)", nombre,
						hashearPwd(pwd), 0, false);
				listaUsuarios = jdbcTemplate.query("select * from usuarios where nombre = ?", new ListarUsuarios(),
						nombre);
				System.out.println("REGISTRO NUEVO: " + listaUsuarios.toString()); // LOG
				Usuario usuarioNuevo = listaUsuarios.get(0);
				return usuarioNuevo;
			} catch (Exception e) {
				System.out.println("REGISTRO fallido");
				return null;
			}
		}
		// Si ya existe el usuario, devolvemos null
		System.out.println("REGISTRO fallido");
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
	@GetMapping("/subirJuego/{idVendedor}/{nombre}/{imagen}/{precio}")
	@CrossOrigin(origins = "*") // Para que se pueda leer en web (HTML)
	public boolean subirJuego(@PathVariable Long idVendedor, @PathVariable String nombre, @PathVariable String imagen,
			@PathVariable double precio) {

		List<Usuario> user = jdbcTemplate.query("SELECT * FROM usuarios WHERE id = ?", new ListarUsuarios(),
				idVendedor);
		if (user.isEmpty()) {
			System.err.println(
					"USUARIO " + idVendedor + " intentó subir un juego --> ERROR: no se ha encontrado usuario");
			return false; // El usuario no ha sido encontrado en la BBDD
		}
		Usuario usuario = user.get(0);
		boolean admin = true;
		BigDecimal precioFinal = BigDecimal.valueOf(precio);
		// Si no es admin, se añade la comisión
		if (!usuario.isAdmin()) {
			admin = false;
			precioFinal = comision(BigDecimal.valueOf(precio));
		}
		// Se genera una calve aleatoria
		String clave = generarClave();
		try {
			jdbcTemplate.update(
					"INSERT INTO juegos(nombre, imagen, precio, clave, vendedor_id, aceptado, revisado, comprador_id) VALUES (?,?,?,?,?,?,?, NULL)",
					nombre, imagen, precioFinal, clave, idVendedor, admin, admin); // Si es admin devuelve true, si es
																					// usuario devuelve false
			System.out.println("USUARIO " + idVendedor + " ha subido un juego");
			return true; // Se ha subido el juego correctamente :
		} catch (Exception e) {
			System.err.println("USUARIO " + idVendedor + " intentó subir un juego --> ERROR: " + e.getMessage());
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
	@GetMapping("/aprobarJuego/{idJuego}/{idUsuario}")
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
			try {
				int fila = jdbcTemplate.update("UPDATE juegos SET aceptado = true, revisado = true WHERE id = ?",
						idJuego);
				if (fila > 0) {
					System.out.println("Juego aprobado por admin --> ID JUEGO: " + idJuego);
					return true; // Juego aprobado
				} else {
					System.err.println("Intento de subir juego por admin -->ERROR: ID JUEGO: " + idJuego);
					return false; // Ha ocurrido un error al aprobar el juego
				}
			} catch (Exception e) {
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

	@GetMapping("/rechazarJuego/{idJuego}/{idUsuario}")
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
	@GetMapping("/misJuegosComprados/{idUsuario}")
	@CrossOrigin(origins = "*") // Para que se pueda leer en web (HTML)
	public List<Juego> misJuegosComprados(@PathVariable Long idUsuario) {
		return jdbcTemplate.query("SELECT * FROM juegos WHERE comprador_id =  ?", new ListarJuegos(), idUsuario);

	}

	@GetMapping("/retrieveSaldo/{idUsuario}")
	@CrossOrigin(origins = "*") // Para que se pueda leer en web (HTML)
	public BigDecimal retrieveSaldo(@PathVariable Long idUsuario) {
		List<Usuario> usuarios = jdbcTemplate.query("SELECT * FROM usuarios WHERE id = ?", new ListarUsuarios(),
				idUsuario);
		if (usuarios.isEmpty()) {
			System.err.println("USUARIO " + idUsuario + "intentó rechazar un juego --> ERROR: NO ES ADMIN");
			return null;
		}
		return usuarios.get(0).getSaldo();
	}

	/**
	 * Endpoint que devuelve la lista de juegos en venta de un usuario
	 * (independientemente de que estén aceptados, pendientes o rechazados)
	 * 
	 * @param idUsuario
	 * @return
	 */
	@GetMapping("/misJuegosEnVenta/{idUsuario}")
	@CrossOrigin(origins = "*") // Para que se pueda leer en web (HTML)
	public List<Juego> misJuegosEnVenta(@PathVariable Long idUsuario) {
		return jdbcTemplate.query("SELECT * FROM juegos WHERE vendedor_id", new ListarJuegos(), idUsuario);
	}

	/**
	 * Endpoint que devuelve el nombre de un usuario dado por su ID
	 * 
	 * @param id
	 * @return el nombre del usuario
	 */
	@GetMapping("/verUsuario/{id}")
	@CrossOrigin(origins = "*") // Para que se pueda leer en web (HTML)
	public String nombreUsuario(@PathVariable Long id) {
		List<Usuario> usuarios = jdbcTemplate.query("select * from usuarios where id = ?", new ListarUsuarios(), id);
		if (usuarios.isEmpty())
			return null;
		return usuarios.get(0).getNombre();
	}

	/**
	 * Endpoint que busca un juego según el filtrado que se le haya dado de búsqueda
	 * 
	 * @param texto
	 * @return Lista de todos los juegos que cumplan el listado de búsqueda
	 */
	@GetMapping("/buscarJuego/{texto}")
	@CrossOrigin(origins = "*") // Para que se pueda leer en web (HTML)
	public List<Juego> buscarJuego(@PathVariable String texto) {
		// Listamos todos los juegos
		List<Juego> listaJuegos = jdbcTemplate.query(
				"select * from juegos where aceptado = true and comprador_id is NULL and LOWER(nombre) like LOWER('%texto%')",
				new ListarJuegos());
		return listaJuegos;
	}

	// ===================== MÉTODOS NO MAPPEADOS ===================== //

	/**
	 * Método que genera una clave aleatoria con la que el usuario podrá canjear su
	 * juego una vez comprado
	 */
	private String generarClave() {
		// Utilizamos la clase UUID que genera una clave random
		String claveAleatoria = UUID.randomUUID().toString();
		try {
			List<Juego> listaJuegos = jdbcTemplate.query("select * from juegos where clave = ?", new ListarJuegos(),
					claveAleatoria);
			// Si la clave coincide con una existente, repetimos proceso hasta que sea
			// diferente
			while (!listaJuegos.isEmpty()) {
				claveAleatoria = UUID.randomUUID().toString();
				listaJuegos = jdbcTemplate.query("select * from juegos where clave = ?", new ListarJuegos(),
						claveAleatoria);
			}
			return claveAleatoria;
		} catch (Exception e) {
			System.err.println("SE INTENTÓ GENERAR UNA CLAVE PARA UN JUEGO NUEVO --> ERROR");
			return null;
		}
	}

	/**
	 * Método que se encarga de poner la comisión correspondiente en cada juego
	 * subido
	 * 
	 * @param precio
	 * @return El precio final al que se va a vender
	 */
	private BigDecimal comision(BigDecimal precio) {
		BigDecimal auxiliar = precio.multiply(COMISION);
		return precio.add(auxiliar);

	}

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
