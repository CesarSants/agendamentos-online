package br.com.cesarsants.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import br.com.cesarsants.dao.generic.IGenericDAO;
import br.com.cesarsants.domain.Agenda;
import br.com.cesarsants.domain.Usuario;
import br.com.cesarsants.exceptions.DAOException;
import br.com.cesarsants.exceptions.TipoChaveNaoEncontradaException;

public interface IAgendaDAO extends IGenericDAO<Agenda, Long> {
    void concluirAgendamento(Agenda agenda) throws TipoChaveNaoEncontradaException, DAOException;
    void cancelarAgendamento(Agenda agenda) throws TipoChaveNaoEncontradaException, DAOException;
    void finalizarAgendamento(Agenda agenda) throws TipoChaveNaoEncontradaException, DAOException;
    void excluirAgendamento(Long agendamentoId) throws TipoChaveNaoEncontradaException, DAOException;
    boolean isMedicoDisponivel(Long medicoId, LocalDateTime horario, Integer duracaoConsulta, Usuario usuario);
    boolean isPacienteDisponivel(Long pacienteId, LocalDateTime horario, Integer duracaoConsulta, Usuario usuario);
    List<Agenda> buscarAgendamentosPorPaciente(Long pacienteId, Usuario usuario);
    List<Agenda> buscarAgendamentosPorMedico(Long medicoId, Usuario usuario);
    List<Agenda> buscarAgendamentosPorClinica(Long clinicaId, Usuario usuario);
    Collection<Agenda> buscarPorSalaEData(Long salaId, LocalDate data, Usuario usuario);
    Collection<Agenda> buscarTodosComRelacionamentos(Usuario usuario) throws DAOException;
    List<Agenda> buscarTodos(Usuario usuario);
    boolean salaTemAgendamentos(Long salaId, Usuario usuario);
    boolean medicoTemAgendamentos(Long medicoId, Usuario usuario);
    boolean pacienteTemAgendamentos(Long pacienteId, Usuario usuario);
    boolean clinicaTemAgendamentos(Long clinicaId, Usuario usuario);
    Collection<Agenda> buscarTodosComRelacionamentosPorUsuarioId(Long usuarioId) throws DAOException;
}