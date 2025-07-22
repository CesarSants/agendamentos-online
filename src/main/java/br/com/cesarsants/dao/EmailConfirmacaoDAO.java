package br.com.cesarsants.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import br.com.cesarsants.dao.generic.GenericDAO;
import br.com.cesarsants.domain.EmailConfirmacao;
import br.com.cesarsants.exceptions.DAOException;
import br.com.cesarsants.service.EntityManagerFactoryService;

public class EmailConfirmacaoDAO extends GenericDAO<EmailConfirmacao, Long> implements IEmailConfirmacaoDAO {
    
    private static EntityManagerFactory getEmf() {
        return EntityManagerFactoryService.getEntityManagerFactory();
    }
    
    public EmailConfirmacaoDAO() {
        super(EmailConfirmacao.class);
    }
    
    public EmailConfirmacao findByToken(String token) throws DAOException {
        EntityManager em = getEmf().createEntityManager();
        try {
            TypedQuery<EmailConfirmacao> query = em.createQuery("SELECT e FROM EmailConfirmacao e WHERE e.token = :token", EmailConfirmacao.class);
            query.setParameter("token", token);
            List<EmailConfirmacao> result = query.getResultList();
            return result.isEmpty() ? null : result.get(0);
        } finally {
            em.close();
        }
    }
    
    @Override
    public EmailConfirmacao buscarPorEmail(String email) throws DAOException {
        EntityManager em = getEmf().createEntityManager();
        try {
            System.out.println("=== DAO: BUSCANDO CONFIRMAÇÃO POR EMAIL ===");
            System.out.println("Email: " + email);
            
            Query query = em.createQuery(
                "SELECT ec FROM EmailConfirmacao ec WHERE ec.email = :email ORDER BY ec.dataCriacao DESC");
            query.setParameter("email", email);
            query.setMaxResults(1);
            List<EmailConfirmacao> resultados = query.getResultList();
            
            System.out.println("Resultados encontrados: " + resultados.size());
            if (!resultados.isEmpty()) {
                EmailConfirmacao confirmacao = resultados.get(0);
                System.out.println("Confirmação encontrada - ID: " + confirmacao.getId() + ", Email: " + confirmacao.getEmail() + ", Código: " + confirmacao.getCodigo());
            }
            
            return resultados.isEmpty() ? null : resultados.get(0);
        } catch (Exception e) {
            System.err.println("Erro ao buscar confirmação por email: " + e.getMessage());
            throw new DAOException("Erro ao buscar confirmação por email: " + email, e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public boolean existeConfirmacaoPendente(String email) throws DAOException {
        EntityManager em = getEmf().createEntityManager();
        try {
            Query query = em.createQuery(
                "SELECT COUNT(ec) FROM EmailConfirmacao ec WHERE ec.email = :email AND ec.confirmado = false AND ec.dataExpiracao > :agora");
            query.setParameter("email", email);
            query.setParameter("agora", java.time.LocalDateTime.now());
            Long count = (Long) query.getSingleResult();
            return count > 0;
        } catch (Exception e) {
            throw new DAOException("Erro ao verificar confirmação pendente para email: " + email, e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public int removerExpirados() throws DAOException {
        EntityManager em = getEmf().createEntityManager();
        try {
            em.getTransaction().begin();
            Query query = em.createQuery(
                "DELETE FROM EmailConfirmacao ec WHERE ec.dataExpiracao < :agora");
            query.setParameter("agora", java.time.LocalDateTime.now());
            int removidos = query.executeUpdate();
            em.getTransaction().commit();
            return removidos;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new DAOException("Erro ao remover confirmações expiradas", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public int removerPorEmail(String email) throws DAOException {
        EntityManager em = getEmf().createEntityManager();
        try {
            em.getTransaction().begin();
            System.out.println("=== REMOVENDO CONFIRMAÇÕES POR EMAIL ===");
            System.out.println("Email: " + email);
            Query query = em.createQuery(
                "DELETE FROM EmailConfirmacao ec WHERE ec.email = :email");
            query.setParameter("email", email);
            int removidos = query.executeUpdate();
            em.getTransaction().commit();
            System.out.println("Registros removidos: " + removidos);
            return removidos;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            System.err.println("Erro ao remover confirmações por email: " + e.getMessage());
            throw new DAOException("Erro ao remover confirmações para email: " + email, e);
        } finally {
            em.close();
        }
    }
} 