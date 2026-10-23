package com.empresa.gestion.service;

import com.empresa.gestion.model.Cliente;
import com.empresa.gestion.model.EstadoPedido;
import com.empresa.gestion.model.Pedido;
import com.empresa.gestion.repository.ClienteRepository;
import com.empresa.gestion.repository.PedidoRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public final class PedidoService {
    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;

    public PedidoService(ClienteRepository clienteRepository, PedidoRepository pedidoRepository) {
        this.clienteRepository = Objects.requireNonNull(clienteRepository, "El repositorio de clientes es obligatorio");
        this.pedidoRepository = Objects.requireNonNull(pedidoRepository, "El repositorio de pedidos es obligatorio");
    }

    public void registrarPedido(Pedido pedido) {
        Objects.requireNonNull(pedido, "El pedido es obligatorio");
        Cliente cliente = clienteRepository.buscarPorId(pedido.clienteId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + pedido.clienteId()));
        if (!cliente.activo()) {
            throw new IllegalStateException("Cliente inactivo: " + cliente.id());
        }
        pedidoRepository.guardar(pedido);
    }

    public List<Pedido> listarPorCliente(int clienteId) {
        return pedidoRepository.listarPorCliente(clienteId);
    }

    /** Sin pedidos devuelve cero, incluso si el cliente no existe (regla del ejercicio). */
    public BigDecimal totalPagadoPorCliente(int clienteId) {
        return pedidoRepository.listarPorCliente(clienteId).stream()
                .filter(pedido -> pedido.estado() == EstadoPedido.PAGADO)
                .map(Pedido::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
