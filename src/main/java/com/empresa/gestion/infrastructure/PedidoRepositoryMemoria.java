package com.empresa.gestion.infrastructure;

import com.empresa.gestion.model.Pedido;
import com.empresa.gestion.repository.PedidoRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Almacenamiento temporal para ejecución en un solo hilo. */
public final class PedidoRepositoryMemoria implements PedidoRepository {
    private final Map<Integer, Pedido> pedidos = new HashMap<>();

    @Override
    public void guardar(Pedido pedido) {
        Objects.requireNonNull(pedido, "El pedido es obligatorio");
        pedidos.put(pedido.id(), pedido);
    }

    @Override
    public List<Pedido> listarPorCliente(int clienteId) {
        return pedidos.values().stream()
                .filter(pedido -> pedido.clienteId() == clienteId)
                .sorted(Comparator.comparingInt(Pedido::id))
                .toList();
    }
}
