package br.com.cesarsants.dao;

import java.util.List;

import javax.persistence.TypedQuery;

import br.com.cesarsants.dao.generic.GenericDAO;
import br.com.cesarsants.domain.Medico;
import br.com.cesarsants.domain.Usuario;

/**
 * @author cesarsants
 *
 */

public class MedicoDAO extends GenericDAO<Medico, Long> implements IMedicoDAO {

    public MedicoDAO() {
        super(Medico.class);
    }

    @Override
    public List<Medico> filtrarMedicos(String query, Usuario usuario) {
        TypedQuery<Medico> tpQuery = 
                this.entityManager.createQuery("SELECT m FROM Medico m WHERE m.usuario = :usuario AND m.nome LIKE :nome", this.persistenteClass);
        tpQuery.setParameter("usuario", usuario);
        tpQuery.setParameter("nome", "%" + query + "%");
        return tpQuery.getResultList();
    }
    
    @Override
    public List<Medico> buscarPorClinica(Long clinicaId, Usuario usuario) {
        String jpql = "SELECT DISTINCT m FROM Medico m JOIN m.clinicasMedico mc WHERE mc.clinica.id = :clinicaId AND m.usuario = :usuario";
        TypedQuery<Medico> query = entityManager.createQuery(jpql, Medico.class);
        query.setParameter("clinicaId", clinicaId);
        query.setParameter("usuario", usuario);
        return query.getResultList();
    }

    public Medico buscarPorCPF(Long cpf, Usuario usuario) {
        try {
            return entityManager.createQuery(
                "SELECT m FROM Medico m WHERE m.cpf = :cpf AND m.usuario = :usuario", Medico.class)
                .setParameter("cpf", cpf)
                .setParameter("usuario", usuario)
                .getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            return null;
        }
    }

    public Medico buscarPorNomeExato(String nome, Usuario usuario) {
        try {
            return entityManager.createQuery(
                "SELECT m FROM Medico m WHERE LOWER(m.nome) = :nome AND m.usuario = :usuario", Medico.class)
                .setParameter("nome", nome.toLowerCase())
                .setParameter("usuario", usuario)
                .getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            return null;
        }
    }
    
    @Override
    public List<Medico> buscarTodos(Usuario usuario) {
        TypedQuery<Medico> query = entityManager.createQuery(
            "SELECT m FROM Medico m WHERE m.usuario = :usuario", Medico.class);
        query.setParameter("usuario", usuario);
        return query.getResultList();
    }
}