package br.com.cesarsants.service;

import br.com.cesarsants.domain.EmailConfirmacao;
import br.com.cesarsants.exceptions.BusinessException;

public interface IEmailConfirmacaoService {
    
    /**
     * Salva um código de confirmação para um email
     * @param email email do usuário
     * @param codigo código de confirmação
     * @return EmailConfirmacao salvo
     */
    EmailConfirmacao salvarCodigo(String email, String codigo) throws BusinessException;
    
    /**
     * Valida um código de confirmação
     * @param email email do usuário
     * @param codigo código a ser validado
     * @return true se o código é válido
     */
    boolean validarCodigo(String email, String codigo) throws BusinessException;
    
    /**
     * Marca uma confirmação como confirmada
     * @param email email do usuário
     * @return true se foi confirmada com sucesso
     */
    boolean confirmarEmail(String email) throws BusinessException;
    
    /**
     * Remove confirmações expiradas
     * @return número de registros removidos
     */
    int limparExpirados() throws BusinessException;
} 