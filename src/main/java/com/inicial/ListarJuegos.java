package com.inicial;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class ListarJuegos implements RowMapper<Juego> {

	@Override
	public Juego mapRow(ResultSet rs, int rowNum) throws SQLException {
		// Mapear uno a uno los usuarios
		Juego juegos = new Juego(rs.getLong("id"), rs.getLong("vendedor_id"), rs.getLong("comprador_id"),
				rs.getString("nombre"), rs.getString("imagen"), rs.getBigDecimal("precio"), rs.getString("clave"),
				rs.getBoolean("aceptado"), rs.getBoolean("revisado"));
//				new SecretKeySpec(rs.getBytes("secretKey"), "AES")); // para crear la secretKey. Hay que pasarla a bytes
		return juegos;
	}

}