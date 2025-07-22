package br.com.cesarsants.service;

import javax.inject.Inject;

import br.com.cesarsants.dao.IUsuarioDAO;
import br.com.cesarsants.domain.Usuario;
import br.com.cesarsants.exceptions.BusinessException;
import br.com.cesarsants.services.generic.GenericService;

/**
 * @author cesarsants
 *
 */
public class UsuarioService extends GenericService<Usuario, Long> implements IUsuarioService {

	private IUsuarioDAO usuarioDAO;

	public UsuarioService(IUsuarioDAO usuarioDAO) {
		super(usuarioDAO);
		this.usuarioDAO = usuarioDAO;
		System.out.println("=== USUARIO SERVICE CONSTRUÍDO ===");
		System.out.println("UsuarioDAO: " + usuarioDAO);
	}

	@Override
	public Usuario buscarPorEmail(String email) {
		System.out.println("=== BUSCANDO POR EMAIL ===");
		System.out.println("Email: " + email);
		System.out.println("UsuarioDAO: " + usuarioDAO);
		
		if (usuarioDAO == null) {
			System.out.println("ERRO: usuarioDAO é null!");
			return null;
		}
		
		Usuario usuario = usuarioDAO.buscarPorEmail(email);
		System.out.println("Usuário encontrado: " + (usuario != null ? usuario.getNome() : "null"));
		return usuario;
	}

	@Override
	public Boolean existePorEmail(String email) {
		System.out.println("=== VERIFICANDO SE EMAIL EXISTE ===");
		System.out.println("Email: " + email);
		System.out.println("UsuarioDAO: " + usuarioDAO);
		
		if (usuarioDAO == null) {
			System.out.println("ERRO: usuarioDAO é null!");
			return false;
		}
		
		Boolean existe = usuarioDAO.existePorEmail(email);
		System.out.println("Email existe: " + existe);
		return existe;
	}

	@Override
	public Usuario autenticar(String email, String senha) throws BusinessException {
		System.out.println("=== AUTENTICANDO USUÁRIO ===");
		System.out.println("Email: " + email);
		System.out.println("Senha: " + senha);
		
		Usuario usuario = buscarPorEmail(email);
		if (usuario == null) {
			System.out.println("Usuário não encontrado");
			throw new BusinessException("Email não encontrado");
		}
		
		System.out.println("Usuário encontrado: " + usuario.getNome());
		System.out.println("Senha do banco: " + usuario.getSenha());
		System.out.println("Senha fornecida: " + senha);
		
		if (!usuario.getSenha().equals(senha)) {
			System.out.println("Senha incorreta");
			throw new BusinessException("Senha incorreta");
		}
		
		System.out.println("Autenticação bem-sucedida");
		return usuario;
	}
} 