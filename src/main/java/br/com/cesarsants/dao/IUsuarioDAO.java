package br.com.cesarsants.dao;

import br.com.cesarsants.dao.generic.IGenericDAO;
import br.com.cesarsants.domain.Usuario;

/**
 * @author cesarsants
 *
 */
public interface IUsuarioDAO extends IGenericDAO<Usuario, Long> {
	
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
} 