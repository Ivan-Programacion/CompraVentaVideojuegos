package com.inicial;

import java.io.Serializable;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Usuario implements Serializable {
	// JsonIgnore --> ignora el atributo a la hora de mandar el JSON por el endpoint
	@JsonIgnore
	private Long id;
	private String nombre;
	@JsonIgnore
	private String pwd;
	private BigDecimal saldo;
	@JsonIgnore
	private boolean admin;

	public Usuario(Long id, String nombre, String pwd, BigDecimal saldo, boolean admin) {
		this.id = id;
		this.nombre = nombre;
		this.pwd = pwd;
		this.saldo = saldo;
		this.admin = admin;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getPwd() {
		return pwd;
	}

	public void setPwd(String pwd) {
		this.pwd = pwd;
	}

	public BigDecimal getSaldo() {
		return saldo;
	}

	public void setSaldo(BigDecimal saldo) {
		this.saldo = saldo;
	}

	public boolean isAdmin() {
		return admin;
	}

	public void setAdmin(boolean admin) {
		this.admin = admin;
	}

	@Override
	public String toString() {
		return "Usuario [id=" + id + ", nombre=" + nombre + ", pwd=" + pwd + ", saldo=" + saldo + ", admin=" + admin
				+ "]";
	}

}
