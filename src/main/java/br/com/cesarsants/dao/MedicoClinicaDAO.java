package br.com.cesarsants.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

import br.com.cesarsants.dao.generic.GenericDAO;
import br.com.cesarsants.domain.MedicoClinica;
import br.com.cesarsants.domain.Usuario;
import br.com.cesarsants.exceptions.DAOException;
import br.com.cesarsants.service.EntityManagerFactoryService;

public class MedicoClinicaDAO extends GenericDAO<MedicoClinica, Long> implements IMedicoClinicaDAO {

    private static EntityManagerFactory getEmf() {
        return EntityManagerFactoryService.getEntityManagerFactory();
    }

    public MedicoClinicaDAO() {
        super(MedicoClinica.class);
    }

    @Override
    public List<MedicoClinica> buscarPorClinica(Long clinicaId, Usuario usuario) {
        EntityManager em = getEmf().createEntityManager();
        try {
            String jpql = "SELECT mc FROM MedicoClinica mc WHERE mc.clinica.id = :clinicaId AND mc.clinica.usuario = :usuario";
            TypedQuery<MedicoClinica> query = em.createQuery(jpql, MedicoClinica.class);
            query.setParameter("clinicaId", clinicaId);
            query.setParameter("usuario", usuario);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public List<MedicoClinica> buscarPorMedico(Long medicoId, Usuario usuario) {
        EntityManager em = getEmf().createEntityManager();
        try {
            String jpql = "SELECT mc FROM MedicoClinica mc WHERE mc.medico.id = :medicoId AND mc.medico.usuario = :usuario";
            TypedQuery<MedicoClinica> query = em.createQuery(jpql, MedicoClinica.class);
            query.setParameter("medicoId", medicoId);
            query.setParameter("usuario", usuario);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Remove o vínculo entre médico e clínica usando DELETE direto
     */
    public void removerVinculo(Long clinicaId, Long medicoId) throws DAOException {
        EntityManager em = getEmf().createEntityManager();
        try {
            em.getTransaction().begin();
            int deleted = em.createQuery("DELETE FROM MedicoClinica mc WHERE mc.clinica.id = :clinicaId AND mc.medico.id = :medicoId")
                .setParameter("clinicaId", clinicaId)
                .setParameter("medicoId", medicoId)
                .executeUpdate();
            em.getTransaction().commit();
            
            if (deleted == 0) {
                throw new DAOException("Vínculo não encontrado para remoção", null);
            }
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new DAOException("Erro ao remover vínculo médico-clínica", e);
        } finally {
            em.close();
        }
    }
}
