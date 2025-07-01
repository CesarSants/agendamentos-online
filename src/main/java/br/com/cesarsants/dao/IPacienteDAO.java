package br.com.cesarsants.dao;

import java.util.List;

import br.com.cesarsants.dao.generic.IGenericDAO;
import br.com.cesarsants.domain.Paciente;
import br.com.cesarsants.domain.Usuario;

/**
 * @author cesarsants
 *
 */

public interface IPacienteDAO extends IGenericDAO<Paciente, Long> {
    List<Paciente> filtrarPacientes(String query, Usuario usuario);
    List<Paciente> buscarPorClinica(Long clinicaId, Usuario usuario);
    Paciente buscarPorCPF(Long cpf, Usuario usuario);
    Paciente buscarPorNomeExato(String nome, Usuario usuario);
    List<Paciente> buscarTodos(Usuario usuario);
}