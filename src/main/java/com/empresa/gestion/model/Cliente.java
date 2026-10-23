package com.empresa.gestion.model;

import java.util.Objects;

public record Cliente(int id, String nombre, boolean activo) {
    public Cliente {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor que cero");
        }
        Objects.requireNonNull(nombre, "El nombre es obligatorio");
        if (nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
    }
}
