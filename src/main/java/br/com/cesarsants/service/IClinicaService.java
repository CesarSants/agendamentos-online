package br.com.cesarsants.service;

import java.time.LocalDateTime;
import java.util.List;

import br.com.cesarsants.domain.Clinica;
import br.com.cesarsants.domain.Usuario;
import br.com.cesarsants.exceptions.DAOException;
import br.com.cesarsants.services.generic.IGenericService;

/**
 * @author cesarsants
 *
 */

public interface IClinicaService extends IGenericService<Clinica, Long> {
    List<Clinica> filtrarClinicas(String query);
    boolean verificarHorarioFuncionamento(Long clinicaId, LocalDateTime horario);
    boolean verificarDisponibilidadeSala(Long clinicaId, Integer numeroSala, LocalDateTime horario, Integer duracaoConsulta);
    void adicionarMedico(Long clinicaId, Long medicoId) throws Exception;
    void removerMedico(Long clinicaId, Long medicoId) throws Exception;
    Clinica buscarPorCNPJ(Long cnpj, Usuario usuario) throws DAOException;
    List<Clinica> buscarPorMedico(Long medicoId);
    List<Clinica> buscarTodos();
    /**
     * Atualiza o número total de salas de uma clínica preservando os dados existentes
     * @param clinicaId ID da clínica
     * @param novoNumeroTotalSalas Novo número total de salas
     * @return Clínica atualizada
     */
    Clinica atualizarNumeroSalas(Long clinicaId, Integer novoNumeroTotalSalas) throws Exception;
}