package br.com.cesarsants.service;

import java.util.List;

import br.com.cesarsants.domain.Medico;
import br.com.cesarsants.domain.Usuario;
import br.com.cesarsants.exceptions.DAOException;
import br.com.cesarsants.services.generic.IGenericService;

public interface IMedicoService extends IGenericService<Medico, Long> {
    Medico buscarPorCPF(Long cpf, Usuario usuario) throws DAOException;
    List<Medico> filtrarMedicos(String query);
    List<Medico> buscarPorClinica(Long clinicaId);
    Medico buscarPorNomeExato(String nome, Usuario usuario);
    List<Medico> buscarTodos();
}