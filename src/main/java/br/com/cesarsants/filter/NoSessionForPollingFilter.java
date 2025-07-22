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

/**
 * Filtro para impedir a criação de sessão HTTP em requisições de polling do sistema de atualização automática.
 * Deve ser o primeiro filtro a rodar.
 */
@WebFilter(filterName = "NoSessionForPollingFilter", urlPatterns = "/*")
public class NoSessionForPollingFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("NoSessionForPollingFilter: Filtro inicializado!");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestURI = httpRequest.getRequestURI();
        String ajaxParam = request.getParameter("javax.faces.partial.ajax");
        String source = request.getParameter("javax.faces.source");

        // Detecta polling do PrimeFaces/JSF
        if (requestURI.contains("/agenda/") && "true".equals(ajaxParam) && source != null && source.contains("poll")) {
            // NÃO cria sessão, apenas segue a cadeia
            // NÃO chame getSession em nenhum momento!
            System.out.println("NoSessionForPollingFilter: Requisição de polling detectada. Não criar/acessar sessão.");
            chain.doFilter(request, response);
            return;
        }
        // Para outras requisições, segue normalmente
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // Limpeza do filtro
    }
} 