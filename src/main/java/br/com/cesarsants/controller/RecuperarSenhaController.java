package br.com.cesarsants.controller;

import java.io.IOException;
import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import br.com.cesarsants.domain.Usuario;
import br.com.cesarsants.exceptions.BusinessException;
import br.com.cesarsants.service.EmailService;
import br.com.cesarsants.service.IEmailConfirmacaoService;
import br.com.cesarsants.service.UsuarioService;

@ManagedBean(name = "recuperarSenhaController")
@SessionScoped
public class RecuperarSenhaController implements Serializable {
    private static final long serialVersionUID = 1L;

    private String email;
    private String novaSenha;
    private String confirmarSenha;

    // Para armazenar temporariamente o email e nova senha até a confirmação
    private String emailParaAlteracao;
    private String novaSenhaParaAlteracao;

    private UsuarioService usuarioService = new UsuarioService(new br.com.cesarsants.dao.UsuarioDAO());

    private final IEmailConfirmacaoService emailConfirmacaoService = new br.com.cesarsants.service.EmailConfirmacaoService();

    private final EmailService emailService = new br.com.cesarsants.service.EmailService();

    @PostConstruct
    public void init() {
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null && facesContext.getExternalContext() != null) {
                facesContext.getExternalContext().getSessionMap().put("recuperacaoSenhaAtiva", true);
            }
        } catch (Exception e) {
            System.err.println("Erro ao inicializar RecuperarSenhaController: " + e.getMessage());
        }
    }

    public String enviar() {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            // Verificar se a sessão ainda é válida
            if (context.getExternalContext().getSession(false) == null) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Sessão expirada", "Sua sessão expirou. Tente novamente."));
                return "/index.xhtml?faces-redirect=true";
            }
            
            // Validação do email
            if (email == null || email.trim().isEmpty()) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo obrigatório", "Digite o email da conta"));
                return null;
            }
            
            // Validação da nova senha
            if (novaSenha == null || novaSenha.trim().isEmpty()) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo obrigatório", "Digite a nova senha"));
                return null;
            }
            
            // Validação da confirmação de senha
            if (confirmarSenha == null || confirmarSenha.trim().isEmpty()) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo obrigatório", "Confirme a nova senha"));
                return null;
            }
            
            // Validação se as senhas coincidem
            if (!novaSenha.equals(confirmarSenha)) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Senhas não conferem", "A nova senha e a confirmação devem ser iguais"));
                return null;
            }
            
            // Validação de segurança da senha
            if (!novaSenha.matches("^(?=.*[A-Za-z])(?=.*\\d).{5,}$")) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Senha inválida", "A senha deve ter pelo menos 5 caracteres, incluindo uma letra e um número"));
                return null;
            }
            
            // Verificar se o email existe no banco
            Usuario usuario = usuarioService.buscarPorEmail(email);
            if (usuario == null) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Email não encontrado", "Não existe uma conta cadastrada com este email"));
                return null;
            }
            
            // Gerar código de confirmação
            String codigo = emailService.gerarCodigoConfirmacao();
            // Enviar email de recuperação de senha
            boolean emailEnviado = emailService.enviarEmailRecuperacaoSenha(email, codigo);
            if (!emailEnviado) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro no envio", "Não foi possível enviar o email de confirmação. Tente novamente."));
                return null;
            }
            // Salvar código no banco
            emailConfirmacaoService.salvarCodigo(email, codigo);
            // Armazenar temporariamente
            this.emailParaAlteracao = email;
            this.novaSenhaParaAlteracao = novaSenha;
            // Salvar dados na sessão para uso na confirmação
            context.getExternalContext().getSessionMap().put("novaSenhaParaAlteracao", novaSenha);
            context.getExternalContext().getSessionMap().put("emailPendente", email);
            // Remover atributo de recuperação antes do redirect
            context.getExternalContext().getSessionMap().remove("recuperacaoSenhaAtiva");
            // Redirecionar para página de confirmação de email
            context.getExternalContext().redirect("confirmacao.xhtml?recuperarSenha=true");
            return null;
        } catch (BusinessException | IOException e) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro interno", "Ocorreu um erro ao processar sua solicitação. Tente novamente."));
            return null;
        }
    }

    // Getters e Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNovaSenha() { return novaSenha; }
    public void setNovaSenha(String novaSenha) { this.novaSenha = novaSenha; }
    public String getConfirmarSenha() { return confirmarSenha; }
    public void setConfirmarSenha(String confirmarSenha) { this.confirmarSenha = confirmarSenha; }
    public String getEmailParaAlteracao() { return emailParaAlteracao; }
    public void setEmailParaAlteracao(String emailParaAlteracao) { this.emailParaAlteracao = emailParaAlteracao; }
    public String getNovaSenhaParaAlteracao() { return novaSenhaParaAlteracao; }
    public void setNovaSenhaParaAlteracao(String novaSenhaParaAlteracao) { this.novaSenhaParaAlteracao = novaSenhaParaAlteracao; }

    public void limparCampos() {
        this.email = null;
        this.novaSenha = null;
        this.confirmarSenha = null;
        // Limpar atributos de sessão se existirem
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            if (context != null && context.getExternalContext() != null) {
                context.getExternalContext().getSessionMap().remove("recuperacaoSenhaAtiva");
            }
        } catch (Exception e) {
            System.err.println("Erro ao limpar sessão: " + e.getMessage());
        }
    }
} 