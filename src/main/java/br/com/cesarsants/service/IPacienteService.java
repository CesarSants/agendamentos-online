package br.com.cesarsants.service;

import java.util.List;

import br.com.cesarsants.domain.Paciente;
import br.com.cesarsants.domain.Usuario;
import br.com.cesarsants.exceptions.DAOException;
import br.com.cesarsants.services.generic.IGenericService;

public interface IPacienteService extends IGenericService<Paciente, Long> {
    Paciente buscarPorCPF(Long cpf, Usuario usuario) throws DAOException;
    List<Paciente> filtrarPacientes(String query);
    List<Paciente> buscarPorClinica(Long clinicaId);
    Paciente buscarPorNomeExato(String nome, Usuario usuario);
    List<Paciente> buscarTodos();
}