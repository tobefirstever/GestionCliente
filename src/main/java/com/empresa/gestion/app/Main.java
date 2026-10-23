package com.empresa.gestion.app;

import com.empresa.gestion.infrastructure.ClienteRepositoryMemoria;
import com.empresa.gestion.infrastructure.PedidoRepositoryMemoria;
import com.empresa.gestion.repository.ClienteRepository;
import com.empresa.gestion.repository.PedidoRepository;
import com.empresa.gestion.service.ClienteService;
import com.empresa.gestion.service.PedidoService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws IOException {
        String modo = args.length == 0 ? "--demo" : args[0];
        if (args.length > 1 || !modo.matches("--(demo|interactivo|help)")) {
            System.err.println("Uso: Main [--demo | --interactivo | --help]");
            System.exit(2);
            return;
        }
        if (modo.equals("--help")) {
            System.out.println("Uso: Main [--demo | --interactivo | --help]");
            return;
        }

        ClienteRepository clientes = new ClienteRepositoryMemoria();
        PedidoRepository pedidos = new PedidoRepositoryMemoria();
        ClienteService clienteService = new ClienteService(clientes);
        PedidoService pedidoService = new PedidoService(clientes, pedidos);

        // Usar la codificación de la terminal cuando existe y UTF-8 para entrada redirigida.
        var terminal = System.console();
        var entrada = terminal == null
                ? new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))
                : new BufferedReader(terminal.reader());
        var salida = terminal == null
                ? new PrintWriter(System.out, true, StandardCharsets.UTF_8)
                : terminal.writer();
        if (modo.equals("--interactivo")) {
            new Consola(clienteService, pedidoService, entrada, salida).ejecutar();
        } else {
            Demo.ejecutar(clienteService, pedidoService, salida);
        }
    }
}
