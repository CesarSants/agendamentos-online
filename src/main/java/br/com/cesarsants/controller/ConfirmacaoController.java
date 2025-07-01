package br.com.cesarsants.controller;

import java.io.Serializable;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import br.com.cesarsants.domain.Usuario;
import br.com.cesarsants.exceptions.BusinessException;
import br.com.cesarsants.exceptions.DAOException;
import br.com.cesarsants.exceptions.TipoChaveNaoEncontradaException;
import br.com.cesarsants.service.EmailService;
import br.com.cesarsants.service.IEmailConfirmacaoService;
import br.com.cesarsants.service.IUsuarioService;

/**
 * @author cesarsants
 *
 */

@Named
@ApplicationScoped
public class ConfirmacaoController implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Inject
    private EmailService emailService;
    
    @Inject
    private IEmailConfirmacaoService emailConfirmacaoService;
    
    @Inject
    private IUsuarioService usuarioService;
    
    private String codigoDigitado;
    private String emailPendente;
    
    public ConfirmacaoController() {
        // Inicialização dos serviços será feita via CDI
    }
    
    /**
     * Solicita confirmação de email
     * @param email email do usuário
     * @param nome nome do usuário
     * @param senha senha do usuário
     * @return resultado da operação
     */
    public String solicitarConfirmacao(String email, String nome, String senha) {
        try {
            System.out.println("=== SOLICITANDO CONFIRMAÇÃO DE EMAIL ===");
            System.out.println("Email: " + email);
            
            // Gerar código de confirmação
            String codigo = emailService.gerarCodigoConfirmacao();
            System.out.println("Código gerado: " + codigo);
            
            // Enviar email
            boolean emailEnviado = emailService.enviarEmailConfirmacaoHTML(email, codigo);
            if (!emailEnviado) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Não foi possível enviar o email de confirmação"));
                return null;
            }
            
            // Salvar código no banco
            emailConfirmacaoService.salvarCodigo(email, codigo);
            
            // Armazenar dados temporariamente na sessão
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("emailPendente", email);
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("nomePendente", nome);
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("senhaPendente", senha);
            
            // Adicionar mensagem de sucesso
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", 
                    "Email de confirmação enviado para " + email + ". Verifique sua caixa de entrada."));
            
            return "/confirmacao.xhtml?faces-redirect=true";
            
        } catch (BusinessException e) {
            System.err.println("Erro ao solicitar confirmação: " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage()));
            return null;
        }
    }
    
    /**
     * Confirma o email com o código fornecido
     * @return resultado da operação
     */
    public String confirmarEmail() {
        try {
            System.out.println("=== CONFIRMANDO EMAIL ===");
            System.out.println("Email: " + emailPendente);
            System.out.println("Código digitado: " + codigoDigitado);
            
            if (codigoDigitado == null || codigoDigitado.trim().isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Digite o código de confirmação"));
                return null;
            }
            
            // Validar código
            boolean codigoValido = emailConfirmacaoService.validarCodigo(emailPendente, codigoDigitado);
            if (!codigoValido) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Código inválido ou expirado"));
                return null;
            }
            
            // Marcar email como confirmado
            boolean confirmado = emailConfirmacaoService.confirmarEmail(emailPendente);
            if (!confirmado) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Não foi possível confirmar o email"));
                return null;
            }
            
            // Recuperar dados da sessão
            String nome = (String) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("nomePendente");
            String senha = (String) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("senhaPendente");
            String novaSenhaParaAlteracao = (String) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("novaSenhaParaAlteracao");

            if (novaSenhaParaAlteracao != null) {
                // Fluxo de recuperação de senha
                Usuario usuario = usuarioService.buscarPorEmail(emailPendente);
                if (usuario != null) {
                    usuario.setSenha(novaSenhaParaAlteracao);
                    usuarioService.alterar(usuario);
                }
                // Limpar dados da sessão
                FacesContext.getCurrentInstance().getExternalContext().getSessionMap().remove("novaSenhaParaAlteracao");
                FacesContext.getCurrentInstance().getExternalContext().getSessionMap().remove("emailPendente");
                // Limpar campos
                codigoDigitado = null;
                emailPendente = null;
                // Mensagem de sucesso
                FacesContext.getCurrentInstance().getExternalContext().getFlash().setKeepMessages(true);
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", 
                        "Senha alterada com sucesso! Faça login com sua nova senha."));
                return "/index.xhtml?faces-redirect=true&modo=login";
            }

            // Fluxo normal de cadastro
            // Criar usuário
            Usuario usuario = new Usuario(nome, emailPendente, senha);
            usuarioService.cadastrar(usuario);
            
            // Limpar dados da sessão
            limparDadosSessao();
            
            // Limpar campos
            codigoDigitado = null;
            emailPendente = null;
            
            // Adicionar mensagem de sucesso
            FacesContext.getCurrentInstance().getExternalContext().getFlash().setKeepMessages(true);
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", 
                    "Email confirmado com sucesso! Sua conta foi criada. Faça login para continuar."));
            
            // Redirecionar para login (modo login)
            return "/index.xhtml?faces-redirect=true&modo=login";
            
        } catch (BusinessException | TipoChaveNaoEncontradaException | DAOException e) {
            System.err.println("Erro ao confirmar email: " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage()));
            return null;
        }
    }
    
    /**
     * Reenvia o código de confirmação
     * @return resultado da operação
     */
    public String reenviarCodigo() {
        try {
            // Carrega o email da sessão se não estiver disponível
            if (emailPendente == null || emailPendente.trim().isEmpty()) {
                emailPendente = (String) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("emailPendente");
            }
            
            if (emailPendente == null || emailPendente.trim().isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Email não encontrado"));
                return null;
            }
            
            System.out.println("=== REENVIANDO CÓDIGO ===");
            System.out.println("Email: " + emailPendente);
            
            // Gerar novo código
            String novoCodigo = emailService.gerarCodigoConfirmacao();
            System.out.println("Novo código gerado: " + novoCodigo);
            
            // Enviar novo email
            boolean emailEnviado = emailService.enviarEmailConfirmacaoHTML(emailPendente, novoCodigo);
            if (!emailEnviado) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Não foi possível reenviar o email"));
                return null;
            }
            
            // Atualizar código no banco (isso vai remover o anterior e criar um novo)
            emailConfirmacaoService.salvarCodigo(emailPendente, novoCodigo);
            
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Novo código enviado para " + emailPendente));
            
            return null;
            
        } catch (BusinessException e) {
            System.err.println("Erro ao reenviar código: " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage()));
            return null;
        }
    }
    
    /**
     * Limpa os dados da sessão
     */
    private void limparDadosSessao() {
        FacesContext.getCurrentInstance().getExternalContext().getSessionMap().remove("emailPendente");
        FacesContext.getCurrentInstance().getExternalContext().getSessionMap().remove("nomePendente");
        FacesContext.getCurrentInstance().getExternalContext().getSessionMap().remove("senhaPendente");
    }
    
    /**
     * Carrega o email pendente da sessão
     */
    public void carregarEmailPendente() {
        if (emailPendente == null) {
            emailPendente = (String) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("emailPendente");
        }
    }
    
    // Getters e Setters
    public String getCodigoDigitado() {
        return codigoDigitado;
    }
    
    public void setCodigoDigitado(String codigoDigitado) {
        this.codigoDigitado = codigoDigitado;
    }
    
    public String getEmailPendente() {
        // Carrega o email da sessão se ainda não foi carregado
        if (emailPendente == null) {
            emailPendente = (String) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("emailPendente");
        }
        return emailPendente;
    }
    
    public void setEmailPendente(String emailPendente) {
        this.emailPendente = emailPendente;
    }
} 