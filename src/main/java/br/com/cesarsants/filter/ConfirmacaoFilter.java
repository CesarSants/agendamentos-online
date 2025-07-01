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

/**
 * @author cesarsants
 *
 */

@WebFilter(urlPatterns = {"/confirmacao.xhtml"})
public class ConfirmacaoFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Inicialização do filtro
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);
        
        // Verifica se existe email pendente na sessão
        boolean temEmailPendente = (session != null && session.getAttribute("emailPendente") != null);
        
        if (temEmailPendente) {
            // Existe email pendente, permite o acesso
            chain.doFilter(request, response);
        } else {
            // Não existe email pendente, redireciona para a página de login
            System.out.println("=== ACESSO NEGADO À PÁGINA DE CONFIRMAÇÃO ===");
            System.out.println("Usuário tentou acessar diretamente: " + httpRequest.getRequestURI());
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/index.xhtml");
        }
    }

    @Override
    public void destroy() {
        // Limpeza do filtro
    }
} 