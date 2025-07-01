package br.com.cesarsants.service;

import java.util.Random;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * @author cesarsants
 *
 */

@ApplicationScoped
public class EmailService {
    
    @Inject
    private GmailEmailService gmailEmailService;
    
    /**
     * Gera um código de confirmação de 6 dígitos
     * @return código de confirmação
     */
    public String gerarCodigoConfirmacao() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(999999));
    }
    
    /**
     * Envia email de confirmação
     * @param email email do destinatário
     * @param codigo código de confirmação
     * @return true se o email foi enviado com sucesso
     */
    public boolean enviarEmailConfirmacao(String email, String codigo) {
        try {
            // Manter logs para testes
            System.out.println("=== EMAIL DE CONFIRMAÇÃO ENVIADO ===");
            System.out.println("Para: " + email);
            System.out.println("Código: " + codigo);
            System.out.println("Assunto: Confirmação de Cadastro - Sistema de Agendamento");
            System.out.println("Mensagem: Seu código de confirmação é: " + codigo);
            System.out.println("Este código expira em 15 minutos.");
            System.out.println("=====================================");
            
            // Enviar email real
            return gmailEmailService.enviarEmailConfirmacao(email, codigo);
        } catch (Exception e) {
            System.err.println("Erro ao enviar email: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Envia email de confirmação com template HTML
     * @param email email do destinatário
     * @param codigo código de confirmação
     * @return true se o email foi enviado com sucesso
     */
    public boolean enviarEmailConfirmacaoHTML(String email, String codigo) {
        try {
            // Manter logs para testes
            System.out.println("=== EMAIL HTML DE CONFIRMAÇÃO ENVIADO ===");
            System.out.println("Para: " + email);
            System.out.println("Código: " + codigo);
            System.out.println("Conteúdo HTML gerado com sucesso");
            System.out.println("=========================================");
            
            // Enviar email real
            return gmailEmailService.enviarEmailConfirmacao(email, codigo);
        } catch (Exception e) {
            System.err.println("Erro ao enviar email HTML: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Envia email de recuperação de senha
     * @param email email do destinatário
     * @param codigo código de confirmação
     * @return true se o email foi enviado com sucesso
     */
    public boolean enviarEmailRecuperacaoSenha(String email, String codigo) {
        try {
            // Manter logs para testes
            System.out.println("=== EMAIL DE RECUPERAÇÃO DE SENHA ENVIADO ===");
            System.out.println("Para: " + email);
            System.out.println("Código: " + codigo);
            System.out.println("Assunto: Recuperação de Senha - Sistema de Agendamento");
            System.out.println("=====================================================");
            
            // Enviar email real
            return gmailEmailService.enviarEmailRecuperacaoSenha(email, codigo);
        } catch (Exception e) {
            System.err.println("Erro ao enviar email de recuperação: " + e.getMessage());
            return false;
        }
    }
    

} 