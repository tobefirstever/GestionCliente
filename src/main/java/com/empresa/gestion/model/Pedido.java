package com.empresa.gestion.model;

import java.math.BigDecimal;
import java.util.Objects;

public record Pedido(int id, int clienteId, BigDecimal monto, EstadoPedido estado) {
    public Pedido {
        if (id <= 0 || clienteId <= 0) {
            throw new IllegalArgumentException("El ID del pedido y del cliente deben ser mayores que cero");
        }
        Objects.requireNonNull(monto, "El monto es obligatorio");
        Objects.requireNonNull(estado, "El estado es obligatorio");
        if (monto.signum() <= 0) {
            throw new IllegalArgumentException("El monto debe ser positivo");
        }
    }
}
