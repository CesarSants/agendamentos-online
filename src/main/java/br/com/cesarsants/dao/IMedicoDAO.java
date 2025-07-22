package br.com.cesarsants.dao;

import java.util.List;

import br.com.cesarsants.dao.generic.IGenericDAO;
import br.com.cesarsants.domain.Medico;
import br.com.cesarsants.domain.Usuario;

public interface IMedicoDAO extends IGenericDAO<Medico, Long> {
    List<Medico> filtrarMedicos(String query, Usuario usuario);
    List<Medico> buscarPorClinica(Long clinicaId, Usuario usuario);
    Medico buscarPorCPF(Long cpf, Usuario usuario);
    Medico buscarPorNomeExato(String nome, Usuario usuario);
    List<Medico> buscarTodos(Usuario usuario);
}