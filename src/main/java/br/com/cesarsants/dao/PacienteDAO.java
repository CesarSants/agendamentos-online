package br.com.cesarsants.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

import br.com.cesarsants.dao.generic.GenericDAO;
import br.com.cesarsants.domain.Paciente;
import br.com.cesarsants.domain.Usuario;
import br.com.cesarsants.exceptions.DAOException;
import br.com.cesarsants.service.EntityManagerFactoryService;

public class PacienteDAO extends GenericDAO<Paciente, Long> implements IPacienteDAO {

    private static EntityManagerFactory getEmf() {
        return EntityManagerFactoryService.getEntityManagerFactory();
    }

    public PacienteDAO() {
        super(Paciente.class);
    }

    @Override
    public List<Paciente> filtrarPacientes(String query, Usuario usuario) {
        EntityManager em = getEmf().createEntityManager();
        try {
            TypedQuery<Paciente> tpQuery = 
                em.createQuery("SELECT p FROM Paciente p WHERE p.usuario = :usuario AND p.nome LIKE :nome", this.persistenteClass);
            tpQuery.setParameter("nome", "%" + query + "%");
            tpQuery.setParameter("usuario", usuario);
            return tpQuery.getResultList();
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<Paciente> buscarPorClinica(Long clinicaId, Usuario usuario) {
        EntityManager em = getEmf().createEntityManager();
        try {
            String jpql = "SELECT DISTINCT p FROM Paciente p JOIN p.agendamentos a WHERE a.clinica.id = :clinicaId AND p.usuario = :usuario";
            TypedQuery<Paciente> query = em.createQuery(jpql, Paciente.class);
            query.setParameter("clinicaId", clinicaId);
            query.setParameter("usuario", usuario);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

//    @Override
//    public Paciente buscarPorCPF(Long cpf) {
//        String jpql = "SELECT p FROM Paciente p WHERE p.cpf = :cpf";
//        TypedQuery<Paciente> query = entityManager.createQuery(jpql, Paciente.class);
//        query.setParameter("cpf", cpf);
//        List<Paciente> result = query.getResultList();
//        return result.isEmpty() ? null : result.get(0);
//    }
    public Paciente buscarPorCPF(Long cpf, Usuario usuario) {
        EntityManager em = getEmf().createEntityManager();
        try {
            try {
                return em.createQuery(
                    "SELECT p FROM Paciente p WHERE p.cpf = :cpf AND p.usuario = :usuario", Paciente.class)
                    .setParameter("cpf", cpf)
                    .setParameter("usuario", usuario)
                    .getSingleResult();
            } catch (javax.persistence.NoResultException e) {
                return null;
            }
        } finally {
            em.close();
        }
    }
    
    @Override
    public Paciente buscarPorNomeExato(String nome, Usuario usuario) {
        EntityManager em = getEmf().createEntityManager();
        try {
            try {
                return em.createQuery(
                    "SELECT p FROM Paciente p WHERE LOWER(p.nome) = :nome AND p.usuario = :usuario", Paciente.class)
                    .setParameter("nome", nome.toLowerCase())
                    .setParameter("usuario", usuario)
                    .setParameter("usuario", usuario)
                    .getSingleResult();
            } catch (javax.persistence.NoResultException e) {
                return null;
            }
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<Paciente> buscarTodos(Usuario usuario) {
        EntityManager em = getEmf().createEntityManager();
        try {
            TypedQuery<Paciente> query = em.createQuery(
                "SELECT p FROM Paciente p WHERE p.usuario = :usuario", Paciente.class);
            query.setParameter("usuario", usuario);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    public Paciente findByCpf(String cpf) throws DAOException {
        EntityManager em = getEmf().createEntityManager();
        try {
            TypedQuery<Paciente> query = em.createQuery("SELECT p FROM Paciente p WHERE p.cpf = :cpf", Paciente.class);
            query.setParameter("cpf", cpf);
            List<Paciente> result = query.getResultList();
            return result.isEmpty() ? null : result.get(0);
        } finally {
            em.close();
        }
    }
}