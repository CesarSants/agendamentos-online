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
 * Filtro para proteger a página de recuperação de senha
 * Só permite acesso se houver um atributo de sessão 'recuperacaoSenhaAtiva'
 */
@WebFilter(urlPatterns = {"/recuperarSenha.xhtml"})
public class RecuperarSenhaFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Inicialização do filtro
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            HttpSession session = httpRequest.getSession(false);
            // NOVO: marca a sessão como pública, se existir
            if (session != null) {
                session.setAttribute("isPublicSession", true);
                session.setMaxInactiveInterval(-1); // nunca expira
            }

            boolean acessoPermitido = (session != null && session.getAttribute("recuperacaoSenhaAtiva") != null);

            if (acessoPermitido) {
                chain.doFilter(request, response);
            } else {
                // Log para debug
                System.out.println("=== ACESSO NEGADO À PÁGINA DE RECUPERAÇÃO DE SENHA ===");
                System.out.println("Usuário tentou acessar diretamente: " + httpRequest.getRequestURI());
                System.out.println("Sessão válida: " + (session != null));
                if (session != null) {
                    System.out.println("Atributo recuperacaoSenhaAtiva: " + session.getAttribute("recuperacaoSenhaAtiva"));
                }
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/index.xhtml");
            }
        } catch (Exception e) {
            System.err.println("Erro no RecuperarSenhaFilter: " + e.getMessage());
            // Em caso de erro, redirecionar para login
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/index.xhtml");
        }
    }

    @Override
    public void destroy() {
        // Limpeza do filtro
    }
} 