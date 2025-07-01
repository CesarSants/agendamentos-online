package br.com.cesarsants.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import br.com.cesarsants.domain.Agenda;
import br.com.cesarsants.exceptions.BusinessException;
import br.com.cesarsants.exceptions.DAOException;
import br.com.cesarsants.exceptions.TipoChaveNaoEncontradaException;
import br.com.cesarsants.services.generic.IGenericService;

/**
 * @author cesarsants
 *
 */

public interface IAgendaService extends IGenericService<Agenda, Long> {
    void concluirAgendamento(Agenda agenda) throws TipoChaveNaoEncontradaException, DAOException;
    void cancelarAgendamento(Agenda agenda) throws TipoChaveNaoEncontradaException, DAOException;
    Agenda criarAgendamento(Long pacienteId, Long medicoId, Long clinicaId, 
                          LocalDateTime dataHora, Integer numeroSala, String observacoes) throws BusinessException;
    List<Agenda> buscarAgendamentosPorPaciente(Long pacienteId);
    List<Agenda> buscarAgendamentosPorMedico(Long medicoId);
    List<Agenda> buscarAgendamentosPorClinica(Long clinicaId);
    boolean verificarDisponibilidade(Long pacienteId, Long medicoId, Long clinicaId, 
                                   LocalDateTime dataHora, Integer numeroSala) throws BusinessException;
    void excluirAgendamento(Long agendamentoId) throws BusinessException;
    void finalizarAgendamento(Long agendamentoId) throws BusinessException;
    Collection<Agenda> buscarPorSalaEData(Long salaId, LocalDate data);
    Collection<Agenda> buscarTodosComRelacionamentos() throws BusinessException;
    List<Agenda> buscarTodos();
    
    // Novos métodos para o mecanismo automático de conclusão
    void concluirAgendamentosAutomaticamente() throws BusinessException;
    Collection<Agenda> buscarAgendamentosParaConclusaoAutomatica() throws BusinessException;
    void concluirAgendamentoAutomatico(Agenda agenda) throws BusinessException;
}