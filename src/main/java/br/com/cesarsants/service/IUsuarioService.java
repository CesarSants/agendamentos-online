package br.com.cesarsants.service;

import br.com.cesarsants.domain.Usuario;
import br.com.cesarsants.exceptions.BusinessException;
import br.com.cesarsants.services.generic.IGenericService;

/**
 * @author cesarsants
 *
 */

public interface IUsuarioService extends IGenericService<Usuario, Long> {
	
	/**
	 * Busca um usuário pelo email
	 * @param email
	 * @return
	 */
	Usuario buscarPorEmail(String email);
	
	/**
	 * Verifica se existe um usuário com o email informado
	 * @param email
	 * @return
	 */
	Boolean existePorEmail(String email);
	
	/**
	 * Autentica um usuário com email e senha
	 * @param email
	 * @param senha
	 * @return
	 * @throws BusinessException
	 */
	Usuario autenticar(String email, String senha) throws BusinessException;
} 