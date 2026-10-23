package com.empresa.gestion.app;

import com.empresa.gestion.infrastructure.ClienteRepositoryMemoria;
import com.empresa.gestion.infrastructure.PedidoRepositoryMemoria;
import com.empresa.gestion.model.Cliente;
import com.empresa.gestion.model.EstadoPedido;
import com.empresa.gestion.model.Pedido;
import com.empresa.gestion.repository.ClienteRepository;
import com.empresa.gestion.repository.PedidoRepository;
import com.empresa.gestion.service.ClienteService;
import com.empresa.gestion.service.PedidoService;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/** Pruebas sin dependencias externas. Cada fallo produce un codigo de salida distinto de cero. */
public final class Pruebas {
    private int ejecutadas;
    private int fallidas;

    private Pruebas() {
    }

    public static void main(String[] args) {
        var suite = new Pruebas();
        suite.ejecutar();
        System.out.printf("%nPruebas: %d | Correctas: %d | Fallidas: %d%n",
                suite.ejecutadas, suite.ejecutadas - suite.fallidas, suite.fallidas);
        if (suite.fallidas > 0) {
            throw new AssertionError("Hay reglas que no cumplen el contrato");
        }
    }

    private void ejecutar() {
        prueba("Cliente: IDs invalidos, nombre nulo o en blanco", () -> {
            for (int id : new int[] {0, -1, Integer.MIN_VALUE}) {
                falla(IllegalArgumentException.class, () -> new Cliente(id, "Carlos", true));
            }
            falla(NullPointerException.class, () -> new Cliente(1, null, true));
            for (String nombre : List.of("", "  ", "\t\n", "\u2003")) {
                falla(IllegalArgumentException.class, () -> new Cliente(1, nombre, true));
            }
            igual("José", new Cliente(1, "José", false).nombre());
        });
        prueba("Pedido: IDs invalidos", () -> {
            for (int id : new int[] {0, -1, Integer.MIN_VALUE}) {
                falla(IllegalArgumentException.class, () -> pedido(id, 1, "1", EstadoPedido.PAGADO));
                falla(IllegalArgumentException.class, () -> pedido(1, id, "1", EstadoPedido.PAGADO));
            }
        });
        prueba("Pedido: importe nulo, cero o negativo y estado nulo", () -> {
            falla(NullPointerException.class, () -> new Pedido(1, 1, null, EstadoPedido.PAGADO));
            falla(NullPointerException.class, () -> pedido(1, 1, "1", null));
            for (String monto : List.of("0", "0.00", "-0.01", "-100")) {
                falla(IllegalArgumentException.class, () -> pedido(1, 1, monto, EstadoPedido.PAGADO));
            }
            decimal("0.001", pedido(1, 1, "0.001", EstadoPedido.PENDIENTE).monto());
        });
        prueba("Clientes: registro, Optional y consulta inexistente", () -> {
            var caso = nuevoCaso();
            igual(true, caso.clientes().buscarPorId(99).isEmpty());
            var cliente = new Cliente(1, "Carlos", true);
            caso.clienteService().registrar(cliente);
            igual(cliente, caso.clienteService().obtenerPorId(1));
            falla(IllegalArgumentException.class, () -> caso.clienteService().obtenerPorId(99));
        });
        prueba("ID de cliente repetido reemplaza el registro", () -> {
            var caso = nuevoCaso();
            caso.clienteService().registrar(new Cliente(1, "Carlos", true));
            caso.clienteService().registrar(new Cliente(1, "Ana", false));
            igual(new Cliente(1, "Ana", false), caso.clienteService().obtenerPorId(1));
        });
        prueba("Cliente inexistente: pedido rechazado sin escritura", () -> {
            var caso = nuevoCaso();
            falla(IllegalArgumentException.class,
                    () -> caso.pedidoService().registrarPedido(pedido(1, 99, "10", EstadoPedido.PAGADO)));
            igual(List.of(), caso.pedidos().listarPorCliente(99));
        });
        prueba("Cliente inactivo: pedido rechazado sin escritura", () -> {
            var caso = nuevoCaso();
            caso.clienteService().registrar(new Cliente(2, "Ana", false));
            falla(IllegalStateException.class,
                    () -> caso.pedidoService().registrarPedido(pedido(1, 2, "10", EstadoPedido.PAGADO)));
            igual(List.of(), caso.pedidos().listarPorCliente(2));
        });
        prueba("Pedidos: filtrado por cliente y orden ascendente", () -> {
            var caso = nuevoCaso();
            caso.clienteService().registrar(new Cliente(1, "Carlos", true));
            caso.clienteService().registrar(new Cliente(2, "Ana", true));
            for (int id : new int[] {103, 101, 102}) {
                caso.pedidoService().registrarPedido(pedido(id, 1, "10", EstadoPedido.PAGADO));
            }
            caso.pedidoService().registrarPedido(pedido(100, 2, "99", EstadoPedido.PAGADO));
            igual(List.of(101, 102, 103), caso.pedidoService().listarPorCliente(1).stream().map(Pedido::id).toList());
            decimal("30", caso.pedidoService().totalPagadoPorCliente(1));
        });
        prueba("Lista no modificable e independiente de cambios posteriores", () -> {
            var repo = new PedidoRepositoryMemoria();
            var primero = pedido(1, 1, "10", EstadoPedido.PAGADO);
            repo.guardar(primero);
            var lista = repo.listarPorCliente(1);
            falla(UnsupportedOperationException.class, lista::clear);
            repo.guardar(pedido(2, 1, "20", EstadoPedido.PAGADO));
            igual(List.of(primero), lista);
            igual(2, repo.listarPorCliente(1).size());
        });
        prueba("ID de pedido repetido reemplaza sin duplicar totales", () -> {
            var caso = nuevoCaso();
            caso.clienteService().registrar(new Cliente(1, "Carlos", true));
            caso.pedidoService().registrarPedido(pedido(101, 1, "120.50", EstadoPedido.PAGADO));
            caso.pedidoService().registrarPedido(pedido(101, 1, "80", EstadoPedido.PENDIENTE));
            igual(1, caso.pedidoService().listarPorCliente(1).size());
            decimal("0", caso.pedidoService().totalPagadoPorCliente(1));
        });
        prueba("Reemplazo rechazado conserva el pedido original", () -> {
            var caso = nuevoCaso();
            caso.clienteService().registrar(new Cliente(1, "Carlos", true));
            var original = pedido(101, 1, "120.50", EstadoPedido.PAGADO);
            caso.pedidoService().registrarPedido(original);
            caso.clienteService().registrar(new Cliente(1, "Carlos", false));
            falla(IllegalStateException.class,
                    () -> caso.pedidoService().registrarPedido(pedido(101, 1, "1", EstadoPedido.PENDIENTE)));
            igual(List.of(original), caso.pedidoService().listarPorCliente(1));
        });
        prueba("Demo: salida exacta y total S/ 170.00", () -> {
            var caso = nuevoCaso();
            var salida = new StringWriter();
            Demo.ejecutar(caso.clienteService(), caso.pedidoService(), new PrintWriter(salida));
            igual("Cliente: Carlos\nPedidos registrados: 3\nTotal pagado: S/ 170.00\n",
                    salida.toString().replace("\r\n", "\n"));
            decimal("170.00", caso.pedidoService().totalPagadoPorCliente(1));
            igual(false, caso.clienteService().obtenerPorId(2).activo());
        });
        prueba("Sin pedidos: lista vacia y cero incluso para cliente inexistente", () -> {
            var caso = nuevoCaso();
            caso.clienteService().registrar(new Cliente(1, "Carlos", true));
            for (int id : new int[] {1, 999, 0, -1}) {
                igual(List.of(), caso.pedidoService().listarPorCliente(id));
                decimal("0", caso.pedidoService().totalPagadoPorCliente(id));
            }
        });
        prueba("BigDecimal: suma exacta y exclusión de pendientes", () -> {
            var caso = nuevoCaso();
            caso.clienteService().registrar(new Cliente(1, "Carlos", true));
            caso.pedidoService().registrarPedido(pedido(1, 1, "0.1", EstadoPedido.PAGADO));
            caso.pedidoService().registrarPedido(pedido(2, 1, "0.2", EstadoPedido.PAGADO));
            caso.pedidoService().registrarPedido(pedido(3, 1, "9999.99", EstadoPedido.PENDIENTE));
            decimal("0.3", caso.pedidoService().totalPagadoPorCliente(1));
        });
        prueba("Dependencias y registros nulos fallan al ingresar", () -> {
            var caso = nuevoCaso();
            falla(NullPointerException.class, () -> new ClienteService(null));
            falla(NullPointerException.class, () -> new PedidoService(null, caso.pedidos()));
            falla(NullPointerException.class, () -> new PedidoService(caso.clientes(), null));
            falla(NullPointerException.class, () -> caso.clienteService().registrar(null));
            falla(NullPointerException.class, () -> caso.pedidoService().registrarPedido(null));
            falla(NullPointerException.class, () -> caso.clientes().guardar(null));
            falla(NullPointerException.class, () -> caso.pedidos().guardar(null));
        });
        prueba("Instancias de repositorios mantienen datos aislados", () -> {
            var primero = nuevoCaso();
            primero.clienteService().registrar(new Cliente(1, "Carlos", true));
            primero.pedidoService().registrarPedido(pedido(1, 1, "10", EstadoPedido.PAGADO));
            var segundo = nuevoCaso();
            igual(true, segundo.clientes().buscarPorId(1).isEmpty());
            igual(List.of(), segundo.pedidos().listarPorCliente(1));
        });
        prueba("Consola: registro y consultas de extremo a extremo", () -> {
            String salida = consola("1\n1\nJosé\ns\n3\n101\n1\n120.50\n2\n"
                    + "3\n102\n1\n80.00\n1\n3\n103\n1\n49.50\n2\n2\n1\n4\n1\n5\n1\n0\n");
            contiene(salida, "1 | José | ACTIVO");
            contiene(salida, "101 | 1 | S/ 120.50 | PAGADO");
            contiene(salida, "102 | 1 | S/ 80.00 | PENDIENTE");
            contiene(salida, "103 | 1 | S/ 49.50 | PAGADO");
            contiene(salida, "Total pagado: S/ 170.00");
            contiene(salida, "Sesión finalizada.");
        });
        prueba("Consola: entradas invalidas permiten continuar", () -> {
            String salida = consola("9\n1\nabc\n1\n-1\n1\n999999999999\n"
                    + "1\n1\nCarlos\nquizas\n1\n1\nCarlos\ns\n"
                    + "3\n1\n1\nxx\n3\n1\n1\n1\n9\n"
                    + "3\n1\n1\n-1\n2\n5\n1\n0\n");
            contiene(salida, "Opción inválida");
            contiene(salida, "El ID debe ser un entero positivo");
            contiene(salida, "El ID debe ser mayor que cero");
            contiene(salida, "Indique s o n");
            contiene(salida, "Ingrese un monto decimal válido");
            contiene(salida, "El estado debe ser 1 o 2");
            contiene(salida, "El monto debe ser positivo");
            contiene(salida, "Total pagado: S/ 0.00");
        });
        prueba("Consola: errores de negocio recuperables y lista vacia", () -> {
            String salida = consola("1\n2\nAna\nn\n3\n1\n2\n10\n2\n"
                    + "3\n1\n99\n10\n2\n2\n99\n4\n2\n0\n");
            contiene(salida, "Cliente inactivo: 2");
            contiene(salida, "Cliente no encontrado: 99");
            contiene(salida, "Sin pedidos para el ID consultado.");
            contiene(salida, "Sesión finalizada.");
        });
        prueba("Consola: EOF en menu o registro termina limpiamente", () -> {
            for (String entrada : List.of("", "1\n", "1\n1\nCarlos\n", "3\n1\n1\n")) {
                String salida = consola(entrada);
                contiene(salida, "Entrada finalizada.");
                contiene(salida, "Sesión finalizada.");
            }
        });
        prueba("Consola: no redondea importes de mas de dos decimales", () -> {
            String salida = consola("1\n1\nCarlos\ns\n3\n1\n1\n0.001\n2\n4\n1\n5\n1\n0\n");
            contiene(salida, "S/ 0.001 | PAGADO");
            contiene(salida, "Total pagado: S/ 0.001");
        });
    }

    private void prueba(String nombre, Verificacion verificacion) {
        ejecutadas++;
        try {
            verificacion.ejecutar();
            System.out.println("OK - " + nombre);
        } catch (Exception | AssertionError error) {
            fallidas++;
            System.err.println("FALLO - " + nombre);
            error.printStackTrace(System.err);
        }
    }

    private static Caso nuevoCaso() {
        ClienteRepository clientes = new ClienteRepositoryMemoria();
        PedidoRepository pedidos = new PedidoRepositoryMemoria();
        return new Caso(clientes, pedidos, new ClienteService(clientes), new PedidoService(clientes, pedidos));
    }

    private static Pedido pedido(int id, int clienteId, String monto, EstadoPedido estado) {
        return new Pedido(id, clienteId, new BigDecimal(monto), estado);
    }

    private static String consola(String entrada) throws Exception {
        var caso = nuevoCaso();
        var salida = new StringWriter();
        new Consola(caso.clienteService(), caso.pedidoService(),
                new BufferedReader(new StringReader(entrada)), new PrintWriter(salida)).ejecutar();
        return salida.toString();
    }

    private static void igual(Object esperado, Object actual) {
        if (!Objects.equals(esperado, actual)) {
            throw new AssertionError("Esperado: " + esperado + "; actual: " + actual);
        }
    }

    private static void decimal(String esperado, BigDecimal actual) {
        if (new BigDecimal(esperado).compareTo(actual) != 0) {
            throw new AssertionError("Esperado: " + esperado + "; actual: " + actual);
        }
    }

    private static void contiene(String texto, String esperado) {
        if (!texto.contains(esperado)) {
            throw new AssertionError("No se encontro: " + esperado + "\nSalida: " + texto);
        }
    }

    private static <T extends Throwable> void falla(Class<T> tipo, Verificacion accion) throws Exception {
        try {
            accion.ejecutar();
        } catch (Exception error) {
            if (tipo.isInstance(error)) {
                return;
            }
            throw new AssertionError("Se esperaba " + tipo.getSimpleName() + ", se recibio " + error, error);
        }
        throw new AssertionError("Se esperaba " + tipo.getSimpleName());
    }

    @FunctionalInterface
    private interface Verificacion {
        void ejecutar() throws Exception;
    }

    private record Caso(ClienteRepository clientes, PedidoRepository pedidos,
                        ClienteService clienteService, PedidoService pedidoService) {
    }
}
