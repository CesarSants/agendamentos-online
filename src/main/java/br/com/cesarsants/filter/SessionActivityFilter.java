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
 * Filtro para gerenciar atividade da sessão e marcar requisições do sistema de atualização automática
 * Executa antes do AuthenticationFilter
 */
@WebFilter("/*")
public class SessionActivityFilter implements Filter {
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Inicialização do filtro
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        System.out.println("SessionActivityFilter: URI=" + httpRequest.getRequestURI() + " | Sessão=" + (httpRequest.getSession(false) != null));
        
        String requestURI = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        boolean isPolling = false;
        boolean isInactivityPing = false;
        boolean isUserActivity = false;
        
        // Polling do sistema de atualização automática da agenda
        if (requestURI.contains("/agenda/")) {
            String ajaxParam = request.getParameter("javax.faces.partial.ajax");
            if ("true".equals(ajaxParam)) {
                String source = request.getParameter("javax.faces.source");
                if (source != null && source.contains("poll")) {
                    request.setAttribute("isSystemPolling", true);
                    isPolling = true;
                    System.out.println("SessionActivityFilter: [POLLING] Requisição do sistema de atualização automática da agenda detectada");
                    System.out.println("Source: " + source);
                    // Não marca como atividade do usuário
                    chain.doFilter(request, response);
                    return;
                }
            }
        }

        // Ping do sistema de inatividade
        String inactivityCheck = request.getParameter("inactivityCheck");
        if ("true".equals(inactivityCheck)) {
            request.setAttribute("isInactivityCheck", true);
            isInactivityPing = true;
            System.out.println("SessionActivityFilter: [INATIVIDADE] Requisição do sistema de inatividade detectada");
            // Não marca como atividade do usuário
            chain.doFilter(request, response);
            return;
        }

        // Só marca atividade real do usuário se houver sessão e usuário logado
        HttpSession session = httpRequest.getSession(false);
        if (session == null) {
            System.out.println("SessionActivityFilter: Nenhuma sessão ativa. Não marca atividade.");
            chain.doFilter(request, response);
            return;
        }
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
        if (usuarioLogado == null) {
            System.out.println("SessionActivityFilter: Nenhum usuário logado. Não marca atividade.");
            chain.doFilter(request, response);
            return;
        }
        String ajaxParam = request.getParameter("javax.faces.partial.ajax");
        if ("true".equals(ajaxParam)) {
            String source = request.getParameter("javax.faces.source");
            String execute = request.getParameter("javax.faces.partial.execute");
            if (source != null && !source.contains("poll") && (execute == null || !execute.contains("poll"))) {
                if (source.contains("btn") || source.contains("button") || 
                    source.contains("form") || source.contains("link") ||
                    source.contains("menu") || source.contains("tab") ||
                    source.contains("dialog") || source.contains("panel")) {
                    request.setAttribute("isUserActivity", true);
                    isUserActivity = true;
                    System.out.println("SessionActivityFilter: [USER] Atividade real do usuário detectada para: " + usuarioLogado.getNome());
                    System.out.println("Source: " + source);
                }
            }
        } else {
            // Navegação direta, refresh, etc.
            request.setAttribute("isUserActivity", true);
            isUserActivity = true;
            System.out.println("SessionActivityFilter: [USER] Navegação detectada para usuário: " + usuarioLogado.getNome());
        }
        if (!isPolling && !isInactivityPing && !isUserActivity) {
            System.out.println("SessionActivityFilter: [OUTRA] Requisição não identificada como polling, ping ou atividade real. Não marca atividade.");
        }
        // Sempre continua a cadeia de filtros
        chain.doFilter(request, response);
    }
    
    @Override
    public void destroy() {
        // Limpeza do filtro
    }
} 