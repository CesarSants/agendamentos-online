package br.com.cesarsants.dao;

import br.com.cesarsants.dao.generic.IGenericDAO;
import br.com.cesarsants.domain.EmailConfirmacao;
import br.com.cesarsants.exceptions.DAOException;

/**
 * @author cesarsants
 *
 */

public interface IEmailConfirmacaoDAO extends IGenericDAO<EmailConfirmacao, Long> {
    
    /**
     * Busca confirmação por email
     * @param email email para buscar
     * @return confirmação encontrada ou null
     * @throws DAOException em caso de erro
     */
    EmailConfirmacao buscarPorEmail(String email) throws DAOException;
    
    /**
     * Verifica se existe confirmação pendente para o email
     * @param email email para verificar
     * @return true se existe confirmação pendente
     * @throws DAOException em caso de erro
     */
    boolean existeConfirmacaoPendente(String email) throws DAOException;
    
    /**
     * Remove todas as confirmações para um email específico
     * @param email email para remover confirmações
     * @return número de registros removidos
     * @throws DAOException em caso de erro
     */
    int removerPorEmail(String email) throws DAOException;
    
    /**
     * Remove confirmações expiradas
     * @return número de registros removidos
     * @throws DAOException em caso de erro
     */
    int removerExpirados() throws DAOException;
} 