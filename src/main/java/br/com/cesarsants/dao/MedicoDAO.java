package br.com.cesarsants.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

import br.com.cesarsants.dao.generic.GenericDAO;
import br.com.cesarsants.domain.Medico;
import br.com.cesarsants.domain.Usuario;
import br.com.cesarsants.service.EntityManagerFactoryService;

public class MedicoDAO extends GenericDAO<Medico, Long> implements IMedicoDAO {

    private static EntityManagerFactory getEmf() {
        return EntityManagerFactoryService.getEntityManagerFactory();
    }

    public MedicoDAO() {
        super(Medico.class);
    }

    @Override
    public List<Medico> filtrarMedicos(String query, Usuario usuario) {
        EntityManager em = getEmf().createEntityManager();
        try {
            TypedQuery<Medico> tpQuery = 
                em.createQuery("SELECT m FROM Medico m WHERE m.usuario = :usuario AND m.nome LIKE :nome", this.persistenteClass);
            tpQuery.setParameter("usuario", usuario);
            tpQuery.setParameter("nome", "%" + query + "%");
            return tpQuery.getResultList();
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<Medico> buscarPorClinica(Long clinicaId, Usuario usuario) {
        EntityManager em = getEmf().createEntityManager();
        try {
            String jpql = "SELECT DISTINCT m FROM Medico m JOIN m.clinicasMedico mc WHERE mc.clinica.id = :clinicaId AND m.usuario = :usuario";
            TypedQuery<Medico> query = em.createQuery(jpql, Medico.class);
            query.setParameter("clinicaId", clinicaId);
            query.setParameter("usuario", usuario);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    public Medico buscarPorCPF(Long cpf, Usuario usuario) {
        EntityManager em = getEmf().createEntityManager();
        try {
            try {
                return em.createQuery(
                    "SELECT m FROM Medico m WHERE m.cpf = :cpf AND m.usuario = :usuario", Medico.class)
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

    public Medico buscarPorNomeExato(String nome, Usuario usuario) {
        EntityManager em = getEmf().createEntityManager();
        try {
            try {
                return em.createQuery(
                    "SELECT m FROM Medico m WHERE LOWER(m.nome) = :nome AND m.usuario = :usuario", Medico.class)
                    .setParameter("nome", nome.toLowerCase())
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
    public List<Medico> buscarTodos(Usuario usuario) {
        EntityManager em = getEmf().createEntityManager();
        try {
            TypedQuery<Medico> query = em.createQuery(
                "SELECT m FROM Medico m WHERE m.usuario = :usuario", Medico.class);
            query.setParameter("usuario", usuario);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
}