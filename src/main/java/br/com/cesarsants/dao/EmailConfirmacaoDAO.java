package br.com.cesarsants.dao;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import br.com.cesarsants.dao.generic.GenericDAO;
import br.com.cesarsants.domain.EmailConfirmacao;
import br.com.cesarsants.exceptions.DAOException;

/**
 * @author cesarsants
 *
 */

@ApplicationScoped
public class EmailConfirmacaoDAO extends GenericDAO<EmailConfirmacao, Long> implements IEmailConfirmacaoDAO {
    
    public EmailConfirmacaoDAO() {
        super(EmailConfirmacao.class);
    }
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public EmailConfirmacao buscarPorEmail(String email) throws DAOException {
        try {
            Query query = entityManager.createQuery(
                "SELECT ec FROM EmailConfirmacao ec WHERE ec.email = :email ORDER BY ec.dataCriacao DESC");
            query.setParameter("email", email);
            query.setMaxResults(1);
            
            List<EmailConfirmacao> resultados = query.getResultList();
            return resultados.isEmpty() ? null : resultados.get(0);
        } catch (Exception e) {
            throw new DAOException("Erro ao buscar confirmação por email: " + email, e);
        }
    }
    
    @Override
    public boolean existeConfirmacaoPendente(String email) throws DAOException {
        try {
            Query query = entityManager.createQuery(
                "SELECT COUNT(ec) FROM EmailConfirmacao ec WHERE ec.email = :email AND ec.confirmado = false AND ec.dataExpiracao > :agora");
            query.setParameter("email", email);
            query.setParameter("agora", java.time.LocalDateTime.now());
            
            Long count = (Long) query.getSingleResult();
            return count > 0;
        } catch (Exception e) {
            throw new DAOException("Erro ao verificar confirmação pendente para email: " + email, e);
        }
    }
    
    @Override
    public int removerExpirados() throws DAOException {
        try {
            Query query = entityManager.createQuery(
                "DELETE FROM EmailConfirmacao ec WHERE ec.dataExpiracao < :agora");
            query.setParameter("agora", java.time.LocalDateTime.now());
            
            return query.executeUpdate();
        } catch (Exception e) {
            throw new DAOException("Erro ao remover confirmações expiradas", e);
        }
    }
    
    @Override
    public int removerPorEmail(String email) throws DAOException {
        try {
            System.out.println("=== REMOVENDO CONFIRMAÇÕES POR EMAIL ===");
            System.out.println("Email: " + email);
            
            Query query = entityManager.createQuery(
                "DELETE FROM EmailConfirmacao ec WHERE ec.email = :email");
            query.setParameter("email", email);
            
            int removidos = query.executeUpdate();
            System.out.println("Registros removidos: " + removidos);
            
            return removidos;
        } catch (Exception e) {
            System.err.println("Erro ao remover confirmações por email: " + e.getMessage());
            throw new DAOException("Erro ao remover confirmações para email: " + email, e);
        }
    }
} 