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

@WebFilter(urlPatterns = {"/index.xhtml"})
public class LoginPageRedirectFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// Inicialização do filtro
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		
		// Sempre cria uma nova sessão HTTP se não existir (evita o limbo)
		HttpSession session = httpRequest.getSession(true);
		// NOVO: marca a sessão como pública
		session.setAttribute("isPublicSession", true);
		// NOVO: desabilita timeout para sessão pública
		session.setMaxInactiveInterval(-1); // nunca expira
		
		// Verifica se o usuário está logado
		boolean isLoggedIn = (session != null && session.getAttribute("usuarioLogado") != null);
		
		if (isLoggedIn) {
			// Usuário já está logado, redireciona para a página de pacientes
			httpResponse.sendRedirect(httpRequest.getContextPath() + "/paciente/list.xhtml");
		} else {
			// Usuário não está logado, permite o acesso à página de login
			// A sessão já foi criada acima, então não há risco de limbo
			chain.doFilter(request, response);
		}
	}

	@Override
	public void destroy() {
		// Limpeza do filtro
	}
} 