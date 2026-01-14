package com.inicial;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class ListarUsuarios implements RowMapper<Usuario> {

	@Override
	public Usuario mapRow(ResultSet rs, int rowNum) throws SQLException {
		// Mapear uno a uno los usuarios
		Usuario usuario = new Usuario(rs.getLong("id"), rs.getString("nombre"), rs.getString("pwd"),
				rs.getBigDecimal("saldo"), rs.getBoolean("admin"));
		return usuario;
	}

}
