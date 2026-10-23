package com.empresa.gestion.repository;

import com.empresa.gestion.model.Cliente;
import java.util.Optional;

public interface ClienteRepository {
    /** Guarda o reemplaza un cliente con el mismo ID, según el contrato del ejercicio. */
    void guardar(Cliente cliente);

    Optional<Cliente> buscarPorId(int id);
}
