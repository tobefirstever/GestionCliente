package com.empresa.gestion.app;

import com.empresa.gestion.model.Cliente;
import com.empresa.gestion.model.EstadoPedido;
import com.empresa.gestion.model.Pedido;
import com.empresa.gestion.service.ClienteService;
import com.empresa.gestion.service.PedidoService;
import java.io.PrintWriter;
import java.math.BigDecimal;

final class Demo {
    private Demo() {
    }

    static void ejecutar(ClienteService clientes, PedidoService pedidos, PrintWriter salida) {
        clientes.registrar(new Cliente(1, "Carlos", true));
        clientes.registrar(new Cliente(2, "Ana", false));
        pedidos.registrarPedido(new Pedido(101, 1, new BigDecimal("120.50"), EstadoPedido.PAGADO));
        pedidos.registrarPedido(new Pedido(102, 1, new BigDecimal("80.00"), EstadoPedido.PENDIENTE));
        pedidos.registrarPedido(new Pedido(103, 1, new BigDecimal("49.50"), EstadoPedido.PAGADO));

        salida.println("Cliente: " + clientes.obtenerPorId(1).nombre());
        salida.println("Pedidos registrados: " + pedidos.listarPorCliente(1).size());
        salida.println("Total pagado: S/ " + pedidos.totalPagadoPorCliente(1).toPlainString());
        salida.flush();
    }
}
