package br.com.cesarsants.controller;

import java.io.Serializable;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpSession;

import br.com.cesarsants.domain.Usuario;
import br.com.cesarsants.exceptions.BusinessException;
import br.com.cesarsants.service.IUsuarioService;

/**
 * @author cesarsants
 *
 */
@Named
@ApplicationScoped
public class AuthController implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private IUsuarioService usuarioService;

	@Inject
	private ConfirmacaoController confirmacaoController;

	private String email;
	private String senha;
	private String nome;
	private String confirmarSenha;
	private boolean modoCadastro = false;

	public String login() {
		System.out.println("=== MÉTODO LOGIN CHAMADO ===");
		System.out.println("Email: " + email);
		System.out.println("Senha: " + senha);
		System.out.println("UsuarioService: " + usuarioService);
		
		try {
			if (usuarioService == null) {
				System.out.println("ERRO: usuarioService é null!");
				FacesContext.getCurrentInstance().addMessage(null, 
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro no login", "Erro interno do sistema"));
				return null;
			}
			
			// Validação unificada dos campos
			if (email == null || email.trim().isEmpty() || senha == null || senha.trim().isEmpty()) {
				FacesContext.getCurrentInstance().addMessage(null, 
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro no login", "Preencha todos os campos"));
				return null;
			}
			
			Usuario usuario = usuarioService.autenticar(email, senha);
			System.out.println("Usuário autenticado: " + usuario.getNome());
			
			// Armazena o usuário na sessão
			FacesContext facesContext = FacesContext.getCurrentInstance();
			HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(false);
			session.setAttribute("usuarioLogado", usuario);
			
			FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_INFO, "Login realizado com sucesso!", "Bem-vindo " + usuario.getNome()));
			
			return "/paciente/list.xhtml?faces-redirect=true";
		} catch (BusinessException e) {
			System.out.println("BusinessException: " + e.getMessage());
			FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro no login", e.getMessage()));
			return null;
		} catch (Exception e) {
			System.out.println("Exception geral: " + e.getMessage());
			e.printStackTrace();
			FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro no login", "Erro interno do sistema"));
			return null;
		}
	}

	public String cadastrar() {
		System.out.println("=== MÉTODO CADASTRAR CHAMADO ===");
		System.out.println("Nome: " + nome);
		System.out.println("Email: " + email);
		System.out.println("Senha: " + senha);
		System.out.println("ConfirmarSenha: " + confirmarSenha);
		System.out.println("UsuarioService: " + usuarioService);
		
		try {
			if (usuarioService == null) {
				System.out.println("ERRO: usuarioService é null!");
				FacesContext.getCurrentInstance().addMessage(null, 
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro no cadastro", "Erro interno do sistema"));
				return null;
			}
			
			// Validação unificada dos campos
			if (nome == null || nome.trim().isEmpty() || 
				email == null || email.trim().isEmpty() || 
				senha == null || senha.trim().isEmpty() || 
				confirmarSenha == null || confirmarSenha.trim().isEmpty()) {
				FacesContext.getCurrentInstance().addMessage(null, 
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro no cadastro", "Preencha todos os campos"));
				return null;
			}
			
			// Validação específica de senhas
			if (!senha.equals(confirmarSenha)) {
				FacesContext.getCurrentInstance().addMessage(null, 
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro no cadastro", "Senhas não conferem"));
				return null;
			}
			
			// Validação de segurança da senha
			if (senha.length() < 5 || !senha.matches(".*[A-Za-z].*") || !senha.matches(".*\\d.*")) {
				FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro no cadastro", "A senha deve ter pelo menos 5 caracteres, pelo menos uma letra e pelo menos um número."));
				return null;
			}
			
			// Verifica se o email já existe
			System.out.println("Verificando se email existe: " + email);
			if (usuarioService.existePorEmail(email)) {
				FacesContext.getCurrentInstance().addMessage(null, 
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro no cadastro", "Email já cadastrado"));
				return null;
			}
			
			// Solicitar confirmação de email em vez de criar usuário diretamente
			System.out.println("Solicitando confirmação de email para: " + email);
			return confirmacaoController.solicitarConfirmacao(email, nome, senha);
			
		} catch (Exception e) {
			System.out.println("Exception geral no cadastro: " + e.getMessage());
			e.printStackTrace();
			FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro no cadastro", "Erro interno do sistema"));
			return null;
		}
	}

	public String voltarParaLogin() {
		System.out.println("=== VOLTANDO PARA LOGIN ===");
		limparCampos();
		modoCadastro = false;
		return "/index.xhtml?faces-redirect=true";
	}

	public String logout() {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(false);
		if (session != null) {
			session.invalidate();
		}
		return "/index.xhtml?faces-redirect=true";
	}

	public Usuario getUsuarioLogado() {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		if (facesContext != null) {
			HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(false);
			if (session != null) {
				return (Usuario) session.getAttribute("usuarioLogado");
			}
		}
		return null;
	}

	public boolean isUsuarioLogado() {
		return getUsuarioLogado() != null;
	}

	public void alternarModo() {
		System.out.println("=== ALTERNANDO MODO ===");
		System.out.println("Modo atual: " + modoCadastro);
		modoCadastro = !modoCadastro;
		System.out.println("Novo modo: " + modoCadastro);
		System.out.println("=== MODO ALTERADO COM SUCESSO ===");
		limparCampos();
	}

	/**
	 * Verifica parâmetros de URL para definir o modo
	 */
	public void verificarParametrosURL() {
		String modo = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("modo");
		if (modo != null && modo.equals("login")) {
			modoCadastro = false;
			System.out.println("Modo definido para LOGIN via parâmetro URL");
		} else if (modo != null && modo.equals("cadastro")) {
			modoCadastro = true;
			System.out.println("Modo definido para CADASTRO via parâmetro URL");
		}
	}

	private void limparCampos() {
		email = null;
		senha = null;
		nome = null;
		confirmarSenha = null;
	}

	public String iniciarRecuperacaoSenha() {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(true);
		session.setAttribute("recuperacaoSenhaAtiva", true);
		return "/recuperarSenha.xhtml?faces-redirect=true";
	}

	// Getters e Setters
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getSenha() {
		return senha;
	}

	public void setSenha(String senha) {
		this.senha = senha;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getConfirmarSenha() {
		return confirmarSenha;
	}

	public void setConfirmarSenha(String confirmarSenha) {
		this.confirmarSenha = confirmarSenha;
	}

	public boolean isModoCadastro() {
		return modoCadastro;
	}

	public void setModoCadastro(boolean modoCadastro) {
		this.modoCadastro = modoCadastro;
	}
} 