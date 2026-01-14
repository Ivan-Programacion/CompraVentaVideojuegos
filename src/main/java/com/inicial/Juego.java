package com.inicial;

import java.io.Serializable;
import java.math.BigDecimal;

public class Juego implements Serializable {

	private Long id;
	private Long vendedor_id;
	private Long comprador_id;
	private String nombre;
	private String imagen;
	private BigDecimal precio;
	private String clave;
	private boolean aceptado;

	public Juego(Long id, Long vendedor_id, Long comprador_id, String nombre, String imagen, BigDecimal precio,
			String clave, boolean aceptado) {
		this.id = id;
		this.vendedor_id = vendedor_id;
		this.comprador_id = comprador_id;
		this.nombre = nombre;
		this.imagen = imagen;
		this.precio = precio;
		this.clave = clave;
		this.aceptado = aceptado;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getVendedor_id() {
		return vendedor_id;
	}

	public void setVendedor_id(Long vendedor_id) {
		this.vendedor_id = vendedor_id;
	}

	public Long getComprador_id() {
		return comprador_id;
	}

	public void setComprador_id(Long comprador_id) {
		this.comprador_id = comprador_id;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getImagen() {
		return imagen;
	}

	public void setImagen(String imagen) {
		this.imagen = imagen;
	}

	public BigDecimal getPrecio() {
		return precio;
	}

	public void setPrecio(BigDecimal precio) {
		this.precio = precio;
	}

	public String getClave() {
		return clave;
	}

	public void setClave(String clave) {
		this.clave = clave;
	}

	public boolean isAceptado() {
		return aceptado;
	}

	public void setAceptado(boolean aceptado) {
		this.aceptado = aceptado;
	}

	@Override
	public String toString() {
		return "Juego [id=" + id + ", vendedor_id=" + vendedor_id + ", comprador_id=" + comprador_id + ", nombre="
				+ nombre + ", imagen=" + imagen + ", precio=" + precio + ", clave=" + clave + ", aceptado=" + aceptado
				+ "]";
	}
}
