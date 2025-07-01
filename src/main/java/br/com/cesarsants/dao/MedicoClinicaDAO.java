package br.com.cesarsants.dao;

import java.util.List;

import javax.persistence.TypedQuery;

import br.com.cesarsants.dao.generic.GenericDAO;
import br.com.cesarsants.domain.MedicoClinica;
import br.com.cesarsants.domain.Usuario;

/**
 * @author cesarsants
 *
 */

public class MedicoClinicaDAO extends GenericDAO<MedicoClinica, Long> implements IMedicoClinicaDAO {

    public MedicoClinicaDAO() {
        super(MedicoClinica.class);
    }

    @Override
    public List<MedicoClinica> buscarPorClinica(Long clinicaId, Usuario usuario) {
        String jpql = "SELECT mc FROM MedicoClinica mc WHERE mc.clinica.id = :clinicaId AND mc.clinica.usuario = :usuario";
        TypedQuery<MedicoClinica> query = entityManager.createQuery(jpql, MedicoClinica.class);
        query.setParameter("clinicaId", clinicaId);
        query.setParameter("usuario", usuario);
        return query.getResultList();
    }

    @Override
    public List<MedicoClinica> buscarPorMedico(Long medicoId, Usuario usuario) {
        String jpql = "SELECT mc FROM MedicoClinica mc WHERE mc.medico.id = :medicoId AND mc.medico.usuario = :usuario";
        TypedQuery<MedicoClinica> query = entityManager.createQuery(jpql, MedicoClinica.class);
        query.setParameter("medicoId", medicoId);
        query.setParameter("usuario", usuario);
        return query.getResultList();
    }
}
