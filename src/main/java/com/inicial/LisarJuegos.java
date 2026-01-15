package com.inicial;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class LisarJuegos implements RowMapper<Juego> {

	@Override
	public Juego mapRow(ResultSet rs, int rowNum) throws SQLException {
		Juego juego = new Juego(rs.getLong("id"), rs.getLong("vendedor_id"), rs.getLong("comprador_id"),
				rs.getString("nombre"), rs.getString("imagen"), rs.getBigDecimal("precio"), rs.getString("clave"),
				rs.getBoolean("aceptado"));
		return juego;
	}

}
