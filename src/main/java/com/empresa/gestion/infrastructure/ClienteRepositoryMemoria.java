package com.empresa.gestion.infrastructure;

import com.empresa.gestion.model.Cliente;
import com.empresa.gestion.repository.ClienteRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Almacenamiento temporal para ejecución en un solo hilo. */
public final class ClienteRepositoryMemoria implements ClienteRepository {
    private final Map<Integer, Cliente> clientes = new HashMap<>();

    @Override
    public void guardar(Cliente cliente) {
        Objects.requireNonNull(cliente, "El cliente es obligatorio");
        clientes.put(cliente.id(), cliente);
    }

    @Override
    public Optional<Cliente> buscarPorId(int id) {
        return Optional.ofNullable(clientes.get(id));
    }
}
