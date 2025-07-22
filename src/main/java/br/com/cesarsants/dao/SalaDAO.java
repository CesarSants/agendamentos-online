package br.com.cesarsants.dao;

import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

import br.com.cesarsants.dao.generic.GenericDAO;
import br.com.cesarsants.domain.Sala;
import br.com.cesarsants.exceptions.DAOException;
import br.com.cesarsants.service.EntityManagerFactoryService;

public class SalaDAO extends GenericDAO<Sala, Long> implements ISalaDAO {
    private static EntityManagerFactory getEmf() {
        return EntityManagerFactoryService.getEntityManagerFactory();
    }

    public SalaDAO() {
        super(Sala.class);
    }

    @Override
    public Collection<Sala> buscarPorClinica(Long clinicaId) {
        EntityManager em = getEmf().createEntityManager();
        try {
            String jpql = "SELECT s FROM Sala s WHERE s.clinica.id = :clinicaId ORDER BY s.ordem";
            TypedQuery<Sala> query = em.createQuery(jpql, Sala.class);
            query.setParameter("clinicaId", clinicaId);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    public Sala findByNome(String nome) throws DAOException {
        EntityManager em = getEmf().createEntityManager();
        try {
            TypedQuery<Sala> query = em.createQuery("SELECT s FROM Sala s WHERE s.nome = :nome", Sala.class);
            query.setParameter("nome", nome);
            List<Sala> result = query.getResultList();
            return result.isEmpty() ? null : result.get(0);
        } finally {
            em.close();
        }
    }
} 