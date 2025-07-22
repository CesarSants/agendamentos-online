package br.com.cesarsants.dao;

import java.time.LocalDateTime;
import java.util.List;

import br.com.cesarsants.dao.generic.IGenericDAO;
import br.com.cesarsants.domain.Clinica;
import br.com.cesarsants.domain.Usuario;

public interface IClinicaDAO extends IGenericDAO<Clinica, Long> {
    List<Clinica> filtrarClinicas(String query, Usuario usuario);
    boolean isSalaDisponivel(Long clinicaId, Integer numeroSala, LocalDateTime horario, Integer duracaoConsulta, Usuario usuario);
    List<Clinica> buscarPorMedico(Long clinicaId, Usuario usuario);
    Clinica buscarPorCNPJ(Long cnpj, Usuario usuario);
    List<Clinica> buscarTodos(Usuario usuario);
}