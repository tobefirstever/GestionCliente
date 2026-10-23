package com.empresa.gestion.repository;

import com.empresa.gestion.model.Pedido;
import java.util.List;

public interface PedidoRepository {
    /** Guarda o reemplaza un pedido con el mismo ID, según el contrato del ejercicio. */
    void guardar(Pedido pedido);

    /** Devuelve una instantánea no modificable, ordenada por ID ascendente. */
    List<Pedido> listarPorCliente(int clienteId);
}
