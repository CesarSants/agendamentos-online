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

@WebFilter(urlPatterns = {"/paciente/*", "/medico/*", "/clinica/*", "/agenda/*"})
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
		HttpSession session = httpRequest.getSession(false);
		
		// Verifica se o usuário está logado
		boolean isLoggedIn = (session != null && session.getAttribute("usuarioLogado") != null);
		
		if (isLoggedIn) {
			// Usuário está logado, permite o acesso
			chain.doFilter(request, response);
		} else {
			// Usuário não está logado, redireciona para a página de login
			httpResponse.sendRedirect(httpRequest.getContextPath() + "/index.xhtml");
		}
	}

	@Override
	public void destroy() {
		// Limpeza do filtro
	}
} 