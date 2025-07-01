package br.com.cesarsants.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import javax.persistence.TypedQuery;

import br.com.cesarsants.dao.generic.GenericDAO;
import br.com.cesarsants.domain.Agenda;
import br.com.cesarsants.domain.Usuario;
import br.com.cesarsants.exceptions.DAOException;
import br.com.cesarsants.exceptions.TipoChaveNaoEncontradaException;

/**
 * @author cesarsants
 *
 */

public class AgendaDAO extends GenericDAO<Agenda, Long> implements IAgendaDAO {

    public AgendaDAO() {
        super(Agenda.class);
    }

    @Override
    public void concluirAgendamento(Agenda agenda) throws TipoChaveNaoEncontradaException, DAOException {
        agenda.setStatus(Agenda.Status.CONCLUIDO);
        super.alterar(agenda);
    }

    @Override
    public void cancelarAgendamento(Agenda agenda) throws TipoChaveNaoEncontradaException, DAOException {
        try {
            agenda.setStatus(Agenda.Status.CANCELADO);
            alterar(agenda);
        } catch (Exception e) {
            throw new DAOException("Erro ao cancelar agendamento", e);
        }
    }
    
    @Override
    public boolean isMedicoDisponivel(Long medicoId, LocalDateTime horario, Integer duracaoConsulta, Usuario usuario) {
        String jpql = "SELECT COUNT(a) FROM Agenda a WHERE a.medico.id = :medicoId " +
                     "AND a.status = 'AGENDADO' " +
                     "AND a.usuario = :usuario " +
                     "AND ((a.dataHora < :fim AND a.dataHoraFim > :inicio))";
        
        LocalDateTime inicio = horario;
        LocalDateTime fim = horario.plusMinutes(duracaoConsulta);
        
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        query.setParameter("medicoId", medicoId);
        query.setParameter("usuario", usuario);
        query.setParameter("inicio", inicio);
        query.setParameter("fim", fim);
        
        return query.getSingleResult() == 0;
    }
    
    @Override
    public boolean isPacienteDisponivel(Long pacienteId, LocalDateTime horario, Integer duracaoConsulta, Usuario usuario) {
        String jpql = "SELECT COUNT(a) FROM Agenda a WHERE a.paciente.id = :pacienteId " +
                     "AND a.status = 'AGENDADO' " +
                     "AND a.usuario = :usuario " +
                     "AND ((a.dataHora < :fim AND a.dataHoraFim > :inicio))";
        
        LocalDateTime inicio = horario;
        LocalDateTime fim = horario.plusMinutes(duracaoConsulta);
        
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        query.setParameter("pacienteId", pacienteId);
        query.setParameter("usuario", usuario);
        query.setParameter("inicio", inicio);
        query.setParameter("fim", fim);
        
        return query.getSingleResult() == 0;
    }

    @Override
    public List<Agenda> buscarAgendamentosPorPaciente(Long pacienteId, Usuario usuario) {
        String jpql = "SELECT a FROM Agenda a WHERE a.paciente.id = :pacienteId AND a.usuario = :usuario ORDER BY a.dataHora";
        TypedQuery<Agenda> query = entityManager.createQuery(jpql, Agenda.class);
        query.setParameter("pacienteId", pacienteId);
        query.setParameter("usuario", usuario);
        return query.getResultList();
    }

    @Override
    public List<Agenda> buscarAgendamentosPorMedico(Long medicoId, Usuario usuario) {
        String jpql = "SELECT a FROM Agenda a WHERE a.medico.id = :medicoId AND a.usuario = :usuario ORDER BY a.dataHora";
        TypedQuery<Agenda> query = entityManager.createQuery(jpql, Agenda.class);
        query.setParameter("medicoId", medicoId);
        query.setParameter("usuario", usuario);
        return query.getResultList();
    }

    @Override
    public List<Agenda> buscarAgendamentosPorClinica(Long clinicaId, Usuario usuario) {
        String jpql = "SELECT a FROM Agenda a WHERE a.clinica.id = :clinicaId AND a.usuario = :usuario ORDER BY a.dataHora";
        TypedQuery<Agenda> query = entityManager.createQuery(jpql, Agenda.class);
        query.setParameter("clinicaId", clinicaId);
        query.setParameter("usuario", usuario);
        return query.getResultList();
    }

    @Override
    public void finalizarAgendamento(Agenda agenda) throws TipoChaveNaoEncontradaException, DAOException {
        try {
            agenda.setStatus(Agenda.Status.CONCLUIDO);
            agenda.setDataHoraFim(LocalDateTime.now());
            alterar(agenda);
        } catch (Exception e) {
            throw new DAOException("Erro ao finalizar agendamento", e);
        }
    }

    @Override
    public void excluirAgendamento(Long agendamentoId) throws TipoChaveNaoEncontradaException, DAOException {
        try {
            entityManager.createQuery("DELETE FROM Agenda a WHERE a.id = :id")
                .setParameter("id", agendamentoId)
                .executeUpdate();
        } catch (Exception e) {
            throw new DAOException("Erro ao excluir agendamento", e);
        }
    }

    @Override
    public Collection<Agenda> buscarPorSalaEData(Long salaId, LocalDate data, Usuario usuario) {
        TypedQuery<Agenda> query = entityManager.createQuery(
            "SELECT a FROM Agenda a WHERE a.sala.id = :salaId AND DATE(a.dataHora) = :data AND a.status = :status AND a.usuario = :usuario",
            Agenda.class);
        query.setParameter("salaId", salaId);
        query.setParameter("data", data);
        query.setParameter("status", Agenda.Status.AGENDADO);
        query.setParameter("usuario", usuario);
        return query.getResultList();
    }

    @Override
    public Collection<Agenda> buscarTodosComRelacionamentos(Usuario usuario) throws DAOException {
        String jpql = "SELECT DISTINCT a FROM Agenda a " +
                     "LEFT JOIN FETCH a.sala " +
                     "LEFT JOIN FETCH a.medico " +
                     "LEFT JOIN FETCH a.paciente " +
                     "LEFT JOIN FETCH a.clinica " +
                     "WHERE a.usuario = :usuario " +
                     "ORDER BY a.dataHora";
        TypedQuery<Agenda> query = entityManager.createQuery(jpql, Agenda.class);
        query.setParameter("usuario", usuario);
        return query.getResultList();
    }
    
    @Override
    public List<Agenda> buscarTodos(Usuario usuario) {
        TypedQuery<Agenda> query = entityManager.createQuery(
            "SELECT a FROM Agenda a WHERE a.usuario = :usuario ORDER BY a.dataHora", Agenda.class);
        query.setParameter("usuario", usuario);
        return query.getResultList();
    }
    
    @Override
    public Collection<Agenda> buscarTodosComRelacionamentosPorUsuarioId(Long usuarioId) throws DAOException {
        String jpql = "SELECT DISTINCT a FROM Agenda a " +
                     "LEFT JOIN FETCH a.sala " +
                     "LEFT JOIN FETCH a.medico " +
                     "LEFT JOIN FETCH a.paciente " +
                     "LEFT JOIN FETCH a.clinica " +
                     "WHERE a.usuario.id = :usuarioId " +
                     "ORDER BY a.dataHora";
        TypedQuery<Agenda> query = entityManager.createQuery(jpql, Agenda.class);
        query.setParameter("usuarioId", usuarioId);
        return query.getResultList();
    }
}