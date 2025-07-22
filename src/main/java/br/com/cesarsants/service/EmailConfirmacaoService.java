package br.com.cesarsants.service;

import br.com.cesarsants.dao.IEmailConfirmacaoDAO;
import br.com.cesarsants.domain.EmailConfirmacao;
import br.com.cesarsants.exceptions.BusinessException;
import br.com.cesarsants.exceptions.DAOException;
import br.com.cesarsants.exceptions.TipoChaveNaoEncontradaException;

public class EmailConfirmacaoService implements IEmailConfirmacaoService {
    
    private IEmailConfirmacaoDAO emailConfirmacaoDAO;

    public EmailConfirmacaoService() {
        this(new br.com.cesarsants.dao.EmailConfirmacaoDAO());
    }

    public EmailConfirmacaoService(IEmailConfirmacaoDAO emailConfirmacaoDAO) {
        this.emailConfirmacaoDAO = emailConfirmacaoDAO;
    }
    
    @Override
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
            System.out.println("=== VALIDANDO CÓDIGO ===");
            System.out.println("Email: " + email);
            System.out.println("Código digitado: " + codigo);
            
            EmailConfirmacao confirmacao = emailConfirmacaoDAO.buscarPorEmail(email);
            
            if (confirmacao == null) {
                System.out.println("Nenhuma confirmação encontrada para o email: " + email);
                return false;
            }
            
            System.out.println("Confirmação encontrada:");
            System.out.println("- ID: " + confirmacao.getId());
            System.out.println("- Email: " + confirmacao.getEmail());
            System.out.println("- Código salvo: " + confirmacao.getCodigo());
            System.out.println("- Código digitado: " + codigo);
            System.out.println("- Códigos iguais: " + confirmacao.getCodigo().equals(codigo));
            System.out.println("- Pode ser confirmado: " + confirmacao.podeSerConfirmado());
            
            // Verifica se o código está correto e não expirou
            boolean resultado = confirmacao.getCodigo().equals(codigo) && confirmacao.podeSerConfirmado();
            System.out.println("Resultado da validação: " + resultado);
            return resultado;
        } catch (DAOException e) {
            System.err.println("Erro ao validar código: " + e.getMessage());
            throw new BusinessException("Erro ao validar código de confirmação", e);
        }
    }
    
    @Override
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
    public int limparExpirados() throws BusinessException {
        try {
            return emailConfirmacaoDAO.removerExpirados();
        } catch (DAOException e) {
            throw new BusinessException("Erro ao limpar confirmações expiradas", e);
        }
    }
} 