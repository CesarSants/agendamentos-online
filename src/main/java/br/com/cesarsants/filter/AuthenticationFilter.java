package br.com.cesarsants.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import br.com.cesarsants.domain.Usuario;

/**
 * Filtro para autenticação e controle de acesso
 */
@WebFilter("/*")
public class AuthenticationFilter implements Filter {
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Inicialização do filtro
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestURI = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        
        // Verifica se é uma página que requer autenticação
        if (isProtectedPage(requestURI, contextPath)) {
            HttpSession session = httpRequest.getSession(false);
            
            if (session == null || session.getAttribute("usuarioLogado") == null) {
                // Usuário não está logado, redireciona para login
                httpResponse.sendRedirect(contextPath + "/index.xhtml");
                return;
            }
            
            // Verifica se o usuário tem permissão para acessar a página
            Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
            if (!hasPermission(usuarioLogado, requestURI, contextPath)) {
                // Usuário não tem permissão, redireciona para página de erro ou login
                httpResponse.sendRedirect(contextPath + "/index.xhtml?error=unauthorized");
                return;
            }
        }
        
        chain.doFilter(request, response);
    }
    
    @Override
    public void destroy() {
        // Limpeza do filtro
    }
    
    /**
     * Verifica se a página requer autenticação
     */
    private boolean isProtectedPage(String requestURI, String contextPath) {
        // Páginas que não requerem autenticação
        String[] publicPages = {
            "/index.xhtml",
            "/confirmacao.xhtml",
            "/recuperarSenha.xhtml",
            "/session/checkStatus.xhtml"
        };
        
        // Remove o context path da URI para comparação
        String relativeURI = requestURI.substring(contextPath.length());
        
        // Verifica se é uma página pública
        for (String publicPage : publicPages) {
            if (relativeURI.equals(publicPage) || relativeURI.startsWith(publicPage + "?")) {
                return false;
            }
        }
        
        // Verifica se é um recurso estático
        if (relativeURI.startsWith("/resources/") || 
            relativeURI.startsWith("/javax.faces.resource/") ||
            relativeURI.endsWith(".css") || 
            relativeURI.endsWith(".js") || 
            relativeURI.endsWith(".png") || 
            relativeURI.endsWith(".jpg") || 
            relativeURI.endsWith(".jpeg") || 
            relativeURI.endsWith(".gif") || 
            relativeURI.endsWith(".ico")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Verifica se o usuário tem permissão para acessar a página
     */
    private boolean hasPermission(Usuario usuario, String requestURI, String contextPath) {
        // Remove o context path da URI para comparação
        String relativeURI = requestURI.substring(contextPath.length());
        
        // Por enquanto, todos os usuários logados têm acesso a todas as páginas
        // Aqui você pode implementar lógica de permissões mais específica
        return true;
    }
} 