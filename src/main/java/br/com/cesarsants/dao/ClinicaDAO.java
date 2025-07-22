package br.com.cesarsants.dao;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

import br.com.cesarsants.dao.generic.GenericDAO;
import br.com.cesarsants.domain.Clinica;
import br.com.cesarsants.domain.Usuario;
import br.com.cesarsants.service.EntityManagerFactoryService;

public class ClinicaDAO extends GenericDAO<Clinica, Long> implements IClinicaDAO {

    private static EntityManagerFactory getEmf() {
        return EntityManagerFactoryService.getEntityManagerFactory();
    }

    public ClinicaDAO() {
        super(Clinica.class);
    }

    @Override
    public List<Clinica> filtrarClinicas(String query, Usuario usuario) {
        EntityManager em = getEmf().createEntityManager();
        try {
            TypedQuery<Clinica> tpQuery = 
                em.createQuery("SELECT c FROM Clinica c WHERE c.usuario = :usuario AND c.nome LIKE :nome", this.persistenteClass);
            tpQuery.setParameter("usuario", usuario);
            tpQuery.setParameter("nome", "%" + query + "%");
            return tpQuery.getResultList();
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<Clinica> buscarTodos(Usuario usuario) {
        EntityManager em = getEmf().createEntityManager();
        try {
            TypedQuery<Clinica> query = em.createQuery(
                "SELECT c FROM Clinica c WHERE c.usuario = :usuario", Clinica.class);
            query.setParameter("usuario", usuario);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<Clinica> buscarPorMedico(Long medicoId, Usuario usuario) {
        EntityManager em = getEmf().createEntityManager();
        try {
            String jpql = "SELECT DISTINCT c FROM Clinica c JOIN c.medicosClinica mc WHERE mc.medico.id = :medicoId AND c.usuario = :usuario";
            TypedQuery<Clinica> query = em.createQuery(jpql, Clinica.class);
            query.setParameter("medicoId", medicoId);
            query.setParameter("usuario", usuario);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public boolean isSalaDisponivel(Long clinicaId, Integer numeroSala, LocalDateTime horario, Integer duracaoConsulta, Usuario usuario) {
        EntityManager em = getEmf().createEntityManager();
        try {
            // Primeiro verifica se o número da sala é válido
            Clinica clinica = em.find(Clinica.class, clinicaId);
            if (clinica == null || numeroSala > clinica.getNumeroTotalSalas()) {
                return false;
            }

            // Verifica se já existe agendamento nesta sala para o horário solicitado
            String jpql = "SELECT COUNT(a) FROM Agenda a " +
                         "WHERE a.clinica.id = :clinicaId " +
                         "AND a.numeroSala = :numeroSala " +
                         "AND a.status = 'AGENDADO' " +
                         "AND a.usuario = :usuario " +
                         "AND ((a.dataHora < :fim AND a.dataHoraFim > :inicio))";
            LocalDateTime inicio = horario;
            LocalDateTime fim = horario.plusMinutes(duracaoConsulta);
            TypedQuery<Long> query = em.createQuery(jpql, Long.class);
            query.setParameter("clinicaId", clinicaId);
            query.setParameter("numeroSala", numeroSala);
            query.setParameter("usuario", usuario);
            query.setParameter("inicio", inicio);
            query.setParameter("fim", fim);
            Long count = query.getSingleResult();
            return count == 0;
        } finally {
            em.close();
        }
    }

    @Override
    public Clinica buscarPorCNPJ(Long cnpj, Usuario usuario) {
        EntityManager em = getEmf().createEntityManager();
        try {
            String jpql = "SELECT c FROM Clinica c WHERE c.cnpj = :cnpj AND c.usuario = :usuario";
            TypedQuery<Clinica> query = em.createQuery(jpql, Clinica.class);
            query.setParameter("cnpj", cnpj);
            query.setParameter("usuario", usuario);
            List<Clinica> resultado = query.getResultList();
            return resultado.isEmpty() ? null : resultado.get(0);
        } finally {
            em.close();
        }
    }
}