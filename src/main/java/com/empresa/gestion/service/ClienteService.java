package com.empresa.gestion.service;

import com.empresa.gestion.model.Cliente;
import com.empresa.gestion.repository.ClienteRepository;
import java.util.Objects;

public final class ClienteService {
    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) {
        this.repository = Objects.requireNonNull(repository, "El repositorio de clientes es obligatorio");
    }

    public void registrar(Cliente cliente) {
        repository.guardar(Objects.requireNonNull(cliente, "El cliente es obligatorio"));
    }

    public Cliente obtenerPorId(int id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + id));
    }
}
