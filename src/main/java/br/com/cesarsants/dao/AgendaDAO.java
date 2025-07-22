package br.com.cesarsants.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

import br.com.cesarsants.dao.generic.GenericDAO;
import br.com.cesarsants.domain.Agenda;
import br.com.cesarsants.domain.Usuario;
import br.com.cesarsants.exceptions.DAOException;
import br.com.cesarsants.exceptions.TipoChaveNaoEncontradaException;
import br.com.cesarsants.service.EntityManagerFactoryService;

public class AgendaDAO extends GenericDAO<Agenda, Long> implements IAgendaDAO {

    private static EntityManagerFactory getEmf() {
        return EntityManagerFactoryService.getEntityManagerFactory();
    }

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
        EntityManager em = getEmf().createEntityManager();
        try {
            String jpql = "SELECT COUNT(a) FROM Agenda a WHERE a.medico.id = :medicoId " +
                         "AND a.status = 'AGENDADO' " +
                         "AND a.usuario = :usuario " +
                         "AND ((a.dataHora < :fim AND a.dataHoraFim > :inicio))";
            LocalDateTime inicio = horario;
            LocalDateTime fim = horario.plusMinutes(duracaoConsulta);
            TypedQuery<Long> query = em.createQuery(jpql, Long.class);
            query.setParameter("medicoId", medicoId);
            query.setParameter("usuario", usuario);
            query.setParameter("inicio", inicio);
            query.setParameter("fim", fim);
            return query.getSingleResult() == 0;
        } finally {
            em.close();
        }
    }
    
    @Override
    public boolean isPacienteDisponivel(Long pacienteId, LocalDateTime horario, Integer duracaoConsulta, Usuario usuario) {
        EntityManager em = getEmf().createEntityManager();
        try {
            String jpql = "SELECT COUNT(a) FROM Agenda a WHERE a.paciente.id = :pacienteId " +
                         "AND a.status = 'AGENDADO' " +
                         "AND a.usuario = :usuario " +
                         "AND ((a.dataHora < :fim AND a.dataHoraFim > :inicio))";
            LocalDateTime inicio = horario;
            LocalDateTime fim = horario.plusMinutes(duracaoConsulta);
            TypedQuery<Long> query = em.createQuery(jpql, Long.class);
            query.setParameter("pacienteId", pacienteId);
            query.setParameter("usuario", usuario);
            query.setParameter("inicio", inicio);
            query.setParameter("fim", fim);
            return query.getSingleResult() == 0;
        } finally {
            em.close();
        }
    }

    @Override
    public List<Agenda> buscarAgendamentosPorPaciente(Long pacienteId, Usuario usuario) {
        EntityManager em = getEmf().createEntityManager();
        try {
            String jpql = "SELECT a FROM Agenda a WHERE a.paciente.id = :pacienteId AND a.usuario = :usuario ORDER BY a.dataHora";
            TypedQuery<Agenda> query = em.createQuery(jpql, Agenda.class);
            query.setParameter("pacienteId", pacienteId);
            query.setParameter("usuario", usuario);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public List<Agenda> buscarAgendamentosPorMedico(Long medicoId, Usuario usuario) {
        EntityManager em = getEmf().createEntityManager();
        try {
            String jpql = "SELECT a FROM Agenda a WHERE a.medico.id = :medicoId AND a.usuario = :usuario ORDER BY a.dataHora";
            TypedQuery<Agenda> query = em.createQuery(jpql, Agenda.class);
            query.setParameter("medicoId", medicoId);
            query.setParameter("usuario", usuario);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public List<Agenda> buscarAgendamentosPorClinica(Long clinicaId, Usuario usuario) {
        EntityManager em = getEmf().createEntityManager();
        try {
            String jpql = "SELECT a FROM Agenda a WHERE a.clinica.id = :clinicaId AND a.usuario = :usuario ORDER BY a.dataHora";
            TypedQuery<Agenda> query = em.createQuery(jpql, Agenda.class);
            query.setParameter("clinicaId", clinicaId);
            query.setParameter("usuario", usuario);
            return query.getResultList();
        } finally {
            em.close();
        }
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
        EntityManager em = getEmf().createEntityManager();
        try {
            em.getTransaction().begin();
            
            // Primeiro, verifica se o agendamento existe e pertence ao usuário logado
            Agenda agenda = em.find(Agenda.class, agendamentoId);
            if (agenda == null) {
                throw new DAOException("Agendamento não encontrado", new Exception());
            }
            
            // Remove o agendamento
            em.remove(agenda);
            em.getTransaction().commit();
            
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new DAOException("Erro ao excluir agendamento: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public Collection<Agenda> buscarPorSalaEData(Long salaId, LocalDate data, Usuario usuario) {
        EntityManager em = getEmf().createEntityManager();
        try {
            TypedQuery<Agenda> query = em.createQuery(
                "SELECT a FROM Agenda a WHERE a.sala.id = :salaId AND DATE(a.dataHora) = :data AND a.status = :status AND a.usuario = :usuario",
                Agenda.class);
            query.setParameter("salaId", salaId);
            query.setParameter("data", data);
            query.setParameter("status", Agenda.Status.AGENDADO);
            query.setParameter("usuario", usuario);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public Collection<Agenda> buscarTodosComRelacionamentos(Usuario usuario) throws DAOException {
        EntityManager em = getEmf().createEntityManager();
        try {
            String jpql = "SELECT DISTINCT a FROM Agenda a " +
                         "LEFT JOIN FETCH a.sala " +
                         "LEFT JOIN FETCH a.medico " +
                         "LEFT JOIN FETCH a.paciente " +
                         "LEFT JOIN FETCH a.clinica " +
                         "WHERE a.usuario = :usuario " +
                         "ORDER BY a.dataHora";
            TypedQuery<Agenda> query = em.createQuery(jpql, Agenda.class);
            query.setParameter("usuario", usuario);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<Agenda> buscarTodos(Usuario usuario) {
        EntityManager em = getEmf().createEntityManager();
        try {
            TypedQuery<Agenda> query = em.createQuery(
                "SELECT a FROM Agenda a WHERE a.usuario = :usuario ORDER BY a.dataHora", Agenda.class);
            query.setParameter("usuario", usuario);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    @Override
    public boolean salaTemAgendamentos(Long salaId, Usuario usuario) {
        EntityManager em = getEmf().createEntityManager();
        try {
            // Verifica agendamentos por ID da sala
            String jpql = "SELECT a FROM Agenda a WHERE a.sala.id = :salaId AND a.usuario = :usuario";
            TypedQuery<Agenda> query = em.createQuery(jpql, Agenda.class);
            query.setParameter("salaId", salaId);
            query.setParameter("usuario", usuario);
            List<Agenda> agendamentos = query.getResultList();
            
            // Verifica também por número da sala (backup)
            String backupJpql = "SELECT COUNT(a) FROM Agenda a WHERE a.numeroSala = " +
                               "(SELECT s.ordem FROM Sala s WHERE s.id = :salaId) " +
                               "AND a.clinica.id = (SELECT s.clinica.id FROM Sala s WHERE s.id = :salaId) " +
                               "AND a.usuario = :usuario";
            TypedQuery<Long> backupQuery = em.createQuery(backupJpql, Long.class);
            backupQuery.setParameter("salaId", salaId);
            backupQuery.setParameter("usuario", usuario);
            Long backupCount = backupQuery.getSingleResult();
            
            return agendamentos.size() > 0 || backupCount > 0;
        } finally {
            em.close();
        }
    }
    
    @Override
    public boolean medicoTemAgendamentos(Long medicoId, Usuario usuario) {
        EntityManager em = getEmf().createEntityManager();
        try {
            String jpql = "SELECT COUNT(a) FROM Agenda a WHERE a.medico.id = :medicoId AND a.usuario = :usuario";
            TypedQuery<Long> query = em.createQuery(jpql, Long.class);
            query.setParameter("medicoId", medicoId);
            query.setParameter("usuario", usuario);
            return query.getSingleResult() > 0;
        } finally {
            em.close();
        }
    }
    
    @Override
    public boolean pacienteTemAgendamentos(Long pacienteId, Usuario usuario) {
        EntityManager em = getEmf().createEntityManager();
        try {
            String jpql = "SELECT COUNT(a) FROM Agenda a WHERE a.paciente.id = :pacienteId AND a.usuario = :usuario";
            TypedQuery<Long> query = em.createQuery(jpql, Long.class);
            query.setParameter("pacienteId", pacienteId);
            query.setParameter("usuario", usuario);
            return query.getSingleResult() > 0;
        } finally {
            em.close();
        }
    }
    
    @Override
    public boolean clinicaTemAgendamentos(Long clinicaId, Usuario usuario) {
        EntityManager em = getEmf().createEntityManager();
        try {
            String jpql = "SELECT COUNT(a) FROM Agenda a WHERE a.clinica.id = :clinicaId AND a.usuario = :usuario";
            TypedQuery<Long> query = em.createQuery(jpql, Long.class);
            query.setParameter("clinicaId", clinicaId);
            query.setParameter("usuario", usuario);
            return query.getSingleResult() > 0;
        } finally {
            em.close();
        }
    }
    
    @Override
    public Collection<Agenda> buscarTodosComRelacionamentosPorUsuarioId(Long usuarioId) throws DAOException {
        EntityManager em = getEmf().createEntityManager();
        try {
            String jpql = "SELECT DISTINCT a FROM Agenda a " +
                         "LEFT JOIN FETCH a.sala " +
                         "LEFT JOIN FETCH a.medico " +
                         "LEFT JOIN FETCH a.paciente " +
                         "LEFT JOIN FETCH a.clinica " +
                         "WHERE a.usuario.id = :usuarioId " +
                         "ORDER BY a.dataHora";
            TypedQuery<Agenda> query = em.createQuery(jpql, Agenda.class);
            query.setParameter("usuarioId", usuarioId);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
}