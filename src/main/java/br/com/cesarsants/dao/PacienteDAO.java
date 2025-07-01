package br.com.cesarsants.dao;

import java.util.List;

import javax.persistence.TypedQuery;

import br.com.cesarsants.dao.generic.GenericDAO;
import br.com.cesarsants.domain.Paciente;
import br.com.cesarsants.domain.Usuario;

/**
 * @author cesarsants
 *
 */

public class PacienteDAO extends GenericDAO<Paciente, Long> implements IPacienteDAO {

    public PacienteDAO() {
        super(Paciente.class);
    }

    @Override
    public List<Paciente> filtrarPacientes(String query, Usuario usuario) {
        TypedQuery<Paciente> tpQuery = 
                this.entityManager.createQuery("SELECT p FROM Paciente p WHERE p.usuario = :usuario AND p.nome LIKE :nome", this.persistenteClass);
        tpQuery.setParameter("nome", "%" + query + "%");
        tpQuery.setParameter("usuario", usuario);
        return tpQuery.getResultList();
    }
    
    @Override
    public List<Paciente> buscarPorClinica(Long clinicaId, Usuario usuario) {
        String jpql = "SELECT DISTINCT p FROM Paciente p JOIN p.agendamentos a WHERE a.clinica.id = :clinicaId AND p.usuario = :usuario";
        TypedQuery<Paciente> query = entityManager.createQuery(jpql, Paciente.class);
        query.setParameter("clinicaId", clinicaId);
        query.setParameter("usuario", usuario);
        return query.getResultList();
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
        try {
            return entityManager.createQuery(
                "SELECT p FROM Paciente p WHERE p.cpf = :cpf AND p.usuario = :usuario", Paciente.class)
                .setParameter("cpf", cpf)
                .setParameter("usuario", usuario)
                .getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            return null;
        }
    }
    
    @Override
    public Paciente buscarPorNomeExato(String nome, Usuario usuario) {
        try {
            return entityManager.createQuery(
                "SELECT p FROM Paciente p WHERE LOWER(p.nome) = :nome AND p.usuario = :usuario", Paciente.class)
                .setParameter("nome", nome.toLowerCase())
                .setParameter("usuario", usuario)
                .getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            return null;
        }
    }
    
    @Override
    public List<Paciente> buscarTodos(Usuario usuario) {
        TypedQuery<Paciente> query = entityManager.createQuery(
            "SELECT p FROM Paciente p WHERE p.usuario = :usuario", Paciente.class);
        query.setParameter("usuario", usuario);
        return query.getResultList();
    }
}