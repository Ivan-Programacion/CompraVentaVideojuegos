package com.inicial;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

public class Exceptions {
	// Manejador de excepciones general
	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> manejarExcGeneral(Exception e) {
		System.err.println(e);
		e.printStackTrace();
		return new ResponseEntity<>("Error " + e, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
