package com.empresa.gestion.app;

import com.empresa.gestion.model.Cliente;
import com.empresa.gestion.model.EstadoPedido;
import com.empresa.gestion.model.Pedido;
import com.empresa.gestion.service.ClienteService;
import com.empresa.gestion.service.PedidoService;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Objects;

/** Adaptador de entrada/salida; las reglas de negocio permanecen en modelos y servicios. */
public final class Consola {
    private final ClienteService clientes;
    private final PedidoService pedidos;
    private final BufferedReader entrada;
    private final PrintWriter salida;

    public Consola(ClienteService clientes, PedidoService pedidos, BufferedReader entrada, PrintWriter salida) {
        this.clientes = Objects.requireNonNull(clientes);
        this.pedidos = Objects.requireNonNull(pedidos);
        this.entrada = Objects.requireNonNull(entrada);
        this.salida = Objects.requireNonNull(salida);
    }

    public void ejecutar() throws IOException {
        salida.println("GESTIÓN DE CLIENTES Y PEDIDOS | Java 21");
        salida.println("Datos en memoria. Un ID repetido reemplaza el registro anterior.");
        try {
            boolean continuar = true;
            while (continuar) {
                salida.println("""

                        1. Registrar cliente
                        2. Consultar cliente
                        3. Registrar pedido
                        4. Listar pedidos de un cliente
                        5. Consultar total pagado
                        0. Salir""");
                try {
                    switch (leer("Opción: ")) {
                        case "1" -> registrarCliente();
                        case "2" -> consultarCliente();
                        case "3" -> registrarPedido();
                        case "4" -> listarPedidos();
                        case "5" -> consultarTotal();
                        case "0" -> continuar = false;
                        default -> salida.println("Opción inválida. Seleccione de 0 a 5.");
                    }
                } catch (IllegalArgumentException | IllegalStateException error) {
                    salida.println("Error: " + error.getMessage());
                }
            }
        } catch (EOFException fin) {
            salida.println("\nEntrada finalizada.");
        }
        salida.println("Sesión finalizada. Los datos en memoria no se conservan.");
        salida.flush();
    }

    private void registrarCliente() throws IOException {
        int id = leerId("ID del cliente: ");
        String nombre = leer("Nombre: ");
        String activo = leer("¿Activo? (s/n): ");
        if (!activo.equalsIgnoreCase("s") && !activo.equalsIgnoreCase("n")) {
            throw new IllegalArgumentException("Indique s o n para el estado del cliente");
        }
        clientes.registrar(new Cliente(id, nombre, activo.equalsIgnoreCase("s")));
        salida.println("Cliente registrado.");
    }

    private void consultarCliente() throws IOException {
        Cliente cliente = clientes.obtenerPorId(leerId("ID del cliente: "));
        salida.printf("%d | %s | %s%n", cliente.id(), cliente.nombre(), cliente.activo() ? "ACTIVO" : "INACTIVO");
    }

    private void registrarPedido() throws IOException {
        int id = leerId("ID del pedido: ");
        int clienteId = leerId("ID del cliente: ");
        BigDecimal monto;
        try {
            monto = new BigDecimal(leer("Monto en soles (ejemplo 120.50, sin separador de miles): "));
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Ingrese un monto decimal válido con punto decimal");
        }
        EstadoPedido estado = switch (leer("Estado (1 = PENDIENTE, 2 = PAGADO): ")) {
            case "1" -> EstadoPedido.PENDIENTE;
            case "2" -> EstadoPedido.PAGADO;
            default -> throw new IllegalArgumentException("El estado debe ser 1 o 2");
        };
        pedidos.registrarPedido(new Pedido(id, clienteId, monto, estado));
        salida.println("Pedido registrado.");
    }

    private void listarPedidos() throws IOException {
        var lista = pedidos.listarPorCliente(leerId("ID del cliente: "));
        if (lista.isEmpty()) {
            salida.println("Sin pedidos para el ID consultado.");
            return;
        }
        salida.println("ID | CLIENTE | MONTO | ESTADO");
        lista.forEach(pedido -> salida.printf("%d | %d | S/ %s | %s%n",
                pedido.id(), pedido.clienteId(), dinero(pedido.monto()), pedido.estado()));
    }

    private void consultarTotal() throws IOException {
        BigDecimal total = pedidos.totalPagadoPorCliente(leerId("ID del cliente: "));
        salida.println("Total pagado: S/ " + dinero(total));
    }

    private int leerId(String mensaje) throws IOException {
        try {
            int id = Integer.parseInt(leer(mensaje));
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("El ID debe ser un entero positivo de hasta 2147483647");
        }
        throw new IllegalArgumentException("El ID debe ser mayor que cero");
    }

    private String leer(String mensaje) throws IOException {
        salida.print(mensaje);
        salida.flush();
        String linea = entrada.readLine();
        if (linea == null) {
            throw new EOFException();
        }
        return linea.strip();
    }

    private static String dinero(BigDecimal valor) {
        // No redondear: el contrato admite importes positivos de cualquier escala.
        return valor.setScale(Math.max(2, valor.scale())).toPlainString();
    }
}
