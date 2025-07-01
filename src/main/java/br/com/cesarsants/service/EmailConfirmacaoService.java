package br.com.cesarsants.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import br.com.cesarsants.dao.IEmailConfirmacaoDAO;
import br.com.cesarsants.domain.EmailConfirmacao;
import br.com.cesarsants.exceptions.BusinessException;
import br.com.cesarsants.exceptions.DAOException;
import br.com.cesarsants.exceptions.TipoChaveNaoEncontradaException;

/**
 * @author cesarsants
 *
 */

@ApplicationScoped
public class EmailConfirmacaoService implements IEmailConfirmacaoService {
    
    @Inject
    private IEmailConfirmacaoDAO emailConfirmacaoDAO;
    
    @Override
    @Transactional
    public EmailConfirmacao salvarCodigo(String email, String codigo) throws BusinessException {
        try {
            System.out.println("=== SALVANDO CÓDIGO ===");
            System.out.println("Email: " + email);
            System.out.println("Código: " + codigo);
            
            // Remove TODAS as confirmações anteriores para o mesmo email
            System.out.println("Removendo confirmações existentes para: " + email);
            emailConfirmacaoDAO.removerPorEmail(email);
            
            // Aguarda um pouco para garantir que a exclusão foi processada
            Thread.sleep(100);
            
            // Cria nova confirmação
            EmailConfirmacao novaConfirmacao = new EmailConfirmacao(email, codigo);
            EmailConfirmacao resultado = emailConfirmacaoDAO.cadastrar(novaConfirmacao);
            System.out.println("Nova confirmação salva com ID: " + resultado.getId());
            return resultado;
        } catch (DAOException | TipoChaveNaoEncontradaException e) {
            System.err.println("Erro ao salvar código: " + e.getMessage());
            throw new BusinessException("Erro ao salvar código de confirmação", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Erro ao processar código de confirmação", e);
        }
    }
    
    @Override
    public boolean validarCodigo(String email, String codigo) throws BusinessException {
        try {
            EmailConfirmacao confirmacao = emailConfirmacaoDAO.buscarPorEmail(email);
            
            if (confirmacao == null) {
                return false;
            }
            
            // Verifica se o código está correto e não expirou
            return confirmacao.getCodigo().equals(codigo) && confirmacao.podeSerConfirmado();
        } catch (DAOException e) {
            throw new BusinessException("Erro ao validar código de confirmação", e);
        }
    }
    
    @Override
    @Transactional
    public boolean confirmarEmail(String email) throws BusinessException {
        try {
            EmailConfirmacao confirmacao = emailConfirmacaoDAO.buscarPorEmail(email);
            
            if (confirmacao == null || !confirmacao.podeSerConfirmado()) {
                return false;
            }
            
            confirmacao.setConfirmado(true);
            emailConfirmacaoDAO.alterar(confirmacao);
            return true;
        } catch (DAOException | TipoChaveNaoEncontradaException e) {
            throw new BusinessException("Erro ao confirmar email", e);
        }
    }
    
    @Override
    @Transactional
    public int limparExpirados() throws BusinessException {
        try {
            return emailConfirmacaoDAO.removerExpirados();
        } catch (DAOException e) {
            throw new BusinessException("Erro ao limpar confirmações expiradas", e);
        }
    }
} 