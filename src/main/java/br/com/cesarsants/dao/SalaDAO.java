package br.com.cesarsants.dao;

import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import br.com.cesarsants.dao.generic.GenericDAO;
import br.com.cesarsants.domain.Sala;

/**
 * @author cesarsants
 *
 */

@ApplicationScoped
public class SalaDAO extends GenericDAO<Sala, Long> implements ISalaDAO {

    @PersistenceContext
    private EntityManager entityManager;

    public SalaDAO() {
        super(Sala.class);
    }

    @Override
    public Collection<Sala> buscarPorClinica(Long clinicaId) {
        String jpql = "SELECT s FROM Sala s WHERE s.clinica.id = :clinicaId ORDER BY s.ordem";
        TypedQuery<Sala> query = entityManager.createQuery(jpql, Sala.class);
        query.setParameter("clinicaId", clinicaId);
        return query.getResultList();
    }
} 