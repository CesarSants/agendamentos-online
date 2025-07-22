package br.com.cesarsants.controller;

import java.io.Serializable;

import javax.faces.bean.SessionScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import br.com.cesarsants.domain.Usuario;
import br.com.cesarsants.service.AutoCompletionSessionManager;

/**
 * Controller para gerenciar verificações de sessão e logout automático
 */
@ManagedBean(name = "sessionController")
@SessionScoped
public class SessionController implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private boolean isAtivo = true;
    private boolean isInsideApp = false;
    private String currentPage = "";
    private boolean pollingAtivo = false;
    
    /**
     * Verifica se a sessão atual ainda é válida
     * @return true se a sessão é válida, false caso contrário
     */
    public boolean isSessionValid() {
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext == null) {
                return false;
            }
            
            HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(false);
            if (session == null) {
                return false;
            }
            
            Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
            return usuarioLogado != null;
            
        } catch (Exception e) {
            System.err.println("Erro ao verificar sessão: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Faz logout automático quando chamado via JavaScript
     */
    public void logoutAutomatico() {
        try {
            System.out.println("=== LOGOUT AUTOMÁTICO SOLICITADO ===");
            
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(false);
                if (session != null) {
                    Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
                    if (usuarioLogado != null) {
                        System.out.println("Logout automático para usuário: " + usuarioLogado.getNome());
                        
                        // Remover usuário do sistema de atualização automática
                        try {
                            AutoCompletionSessionManager sessionManager = AutoCompletionSessionManager.getInstance();
                            sessionManager.removerUsuarioAtivo(usuarioLogado.getId());
                            System.out.println("Usuário removido do sistema de atualização automática");
                        } catch (Exception e) {
                            System.err.println("Erro ao remover usuário do sistema de atualização: " + e.getMessage());
                        }
                    }
                    
                    // Invalidar a sessão
                    session.invalidate();
                    System.out.println("Sessão invalidada com sucesso");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Erro no logout automático: " + e.getMessage());
        }
    }
    
    /**
     * Retorna true se a sessão é válida, false caso contrário
     * Pode ser usado por JavaScript para verificar o status da sessão
     */
    public String getSessionStatus() {
        return isSessionValid() ? "VALID" : "EXPIRED";
    }
    
    /**
     * Estende a sessão atual (atualiza o lastAccessedTime)
     * Pode ser chamado periodicamente para manter a sessão ativa
     */
    public void extendSession() {
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext == null) {
                return;
            }
            
            // Verifica se é uma atividade real do usuário (definido pelo filtro)
            HttpServletRequest request = (HttpServletRequest) facesContext.getExternalContext().getRequest();
            Boolean isUserActivity = (Boolean) request.getAttribute("isUserActivity");
            
            if (isUserActivity == null || !isUserActivity) {
                // Não estende a sessão para requisições que não são atividade real do usuário
                System.out.println("ExtendSession: Requisição não é atividade real do usuário - não estendendo sessão");
                return;
            }
            
            // Estende a sessão apenas para atividades reais do usuário
            System.out.println("ExtendSession: Atividade real do usuário detectada - estendendo sessão");
            
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.setAttribute("lastActivity", System.currentTimeMillis());
                System.out.println("Sessão estendida - último acesso: " + new java.util.Date());
            }
        } catch (Exception e) {
            System.err.println("Erro ao estender sessão: " + e.getMessage());
        }
    }
    
    /**
     * Verifica o status da sessão via AJAX
     * Usado pelo JavaScript para verificar periodicamente se a sessão ainda é válida
     */
    public String checkSessionStatus() {
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext == null) {
                return "/index.xhtml?sessionExpired=true&faces-redirect=true";
            }
            
            // Verifica se é uma atividade real do usuário (definido pelo filtro)
            HttpServletRequest request = (HttpServletRequest) facesContext.getExternalContext().getRequest();
            Boolean isUserActivity = (Boolean) request.getAttribute("isUserActivity");
            
            if (isUserActivity == null || !isUserActivity) {
                // Não estende a sessão para requisições que não são atividade real do usuário
                System.out.println("CheckStatus: Requisição não é atividade real do usuário - não estendendo sessão");
                return null;
            }
            
            // Estende a sessão apenas para atividades reais do usuário
            System.out.println("CheckStatus: Atividade real do usuário detectada - estendendo sessão");
            
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.setAttribute("lastActivity", System.currentTimeMillis());
                System.out.println("Sessão estendida - último acesso: " + new java.util.Date());
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("Erro ao verificar status da sessão: " + e.getMessage());
            return "/index.xhtml?sessionExpired=true&faces-redirect=true";
        }
    }

    /**
     * Garante que a sessão HTTP exista (usado para evitar o limbo na tela de login)
     */
    public String garantirSessao() {
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext == null) {
                return null;
            }
            
            // Verifica se é uma atividade real do usuário (definido pelo filtro)
            HttpServletRequest request = (HttpServletRequest) facesContext.getExternalContext().getRequest();
            Boolean isUserActivity = (Boolean) request.getAttribute("isUserActivity");
            
            if (isUserActivity == null || !isUserActivity) {
                // Não estende a sessão para requisições que não são atividade real do usuário
                System.out.println("GarantirSessao: Requisição não é atividade real do usuário - não estendendo sessão");
                return null;
            }
            
            // Estende a sessão apenas para atividades reais do usuário
            System.out.println("GarantirSessao: Atividade real do usuário detectada - estendendo sessão");
            
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.setAttribute("lastActivity", System.currentTimeMillis());
                System.out.println("Sessão estendida - último acesso: " + new java.util.Date());
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("Erro ao garantir sessão: " + e.getMessage());
            return null;
        }
    }

    public boolean isAtivo() {
        return isAtivo;
    }
    public void setAtivo(boolean ativo) {
        this.isAtivo = ativo;
    }
    public boolean isInsideApp() {
        return isInsideApp;
    }
    public void setInsideApp(boolean insideApp) {
        this.isInsideApp = insideApp;
        System.out.println("[FRONT] Polling " + (insideApp ? "ATIVADO" : "DESATIVADO"));
    }
    public String getCurrentPage() {
        return currentPage;
    }
    public void setCurrentPage(String currentPage) {
        this.currentPage = currentPage;
    }
    public boolean isPollingAtivo() {
        return pollingAtivo;
    }
    public void setPollingAtivo(boolean pollingAtivo) {
        this.pollingAtivo = pollingAtivo;
        System.out.println("[FRONT] Polling " + (pollingAtivo ? "ATIVADO" : "DESATIVADO"));
    }

    /**
     * Atualiza o estado da sessão e as variáveis de controle para o frontend
     */
    public void atualizarEstadoSessao(HttpServletRequest request) {
        String path = request.getServletPath();
        setCurrentPage(path);

        // Páginas internas do app
        boolean insideApp = path.startsWith("/paciente/") || path.startsWith("/medico/") ||
                            path.startsWith("/clinica/") || path.startsWith("/agenda/");
        setInsideApp(insideApp);

        // Sessão ativa se usuário logado
        HttpSession session = request.getSession(false);
        boolean ativo = (session != null && session.getAttribute("usuarioLogado") != null);
        setAtivo(ativo);

        // Polling ativo apenas se usuário está logado E está dentro do app
        setPollingAtivo(ativo && insideApp);
    }
    
    /**
     * Verifica se o polling deve estar ativo baseado no estado atual
     * @return true se o polling deve estar ativo, false caso contrário
     */
    public boolean deveAtivarPolling() {
        return isAtivo() && isInsideApp();
    }
} 