package br.com.cesarsants.dao;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import br.com.cesarsants.dao.generic.GenericDAO;
import br.com.cesarsants.domain.Usuario;

/**
 * @author cesarsants
 *
 */

@ApplicationScoped
public class UsuarioDAO extends GenericDAO<Usuario, Long> implements IUsuarioDAO {

	@PersistenceContext
	private EntityManager entityManager;

	public UsuarioDAO() {
		super(Usuario.class);
		System.out.println("=== USUARIO DAO CONSTRUÍDO ===");
		System.out.println("EntityManager: " + entityManager);
	}

	@Override
	public Usuario buscarPorEmail(String email) {
		System.out.println("=== DAO: BUSCANDO POR EMAIL ===");
		System.out.println("Email: " + email);
		System.out.println("EntityManager: " + entityManager);
		
		if (entityManager == null) {
			System.out.println("ERRO: entityManager é null!");
			return null;
		}
		
		try {
			Usuario usuario = entityManager.createQuery(
				"SELECT u FROM Usuario u WHERE u.email = :email", Usuario.class)
				.setParameter("email", email)
				.getSingleResult();
			System.out.println("Usuário encontrado no banco: " + usuario.getNome());
			return usuario;
		} catch (javax.persistence.NoResultException e) {
			System.out.println("Nenhum usuário encontrado com este email");
			return null;
		} catch (Exception e) {
			System.out.println("Erro ao buscar usuário: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Boolean existePorEmail(String email) {
		System.out.println("=== DAO: VERIFICANDO SE EMAIL EXISTE ===");
		System.out.println("Email: " + email);
		System.out.println("EntityManager: " + entityManager);
		
		if (entityManager == null) {
			System.out.println("ERRO: entityManager é null!");
			return false;
		}
		
		try {
			Long count = entityManager.createQuery(
				"SELECT COUNT(u) FROM Usuario u WHERE u.email = :email", Long.class)
				.setParameter("email", email)
				.getSingleResult();
			System.out.println("Quantidade de usuários com este email: " + count);
			return count > 0;
		} catch (javax.persistence.NoResultException e) {
			System.out.println("Nenhum resultado encontrado");
			return false;
		} catch (Exception e) {
			System.out.println("Erro ao verificar email: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}
} 