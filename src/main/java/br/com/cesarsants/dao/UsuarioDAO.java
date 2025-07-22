package br.com.cesarsants.dao;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

import br.com.cesarsants.dao.generic.GenericDAO;
import br.com.cesarsants.domain.Usuario;
import br.com.cesarsants.exceptions.DAOException;
import br.com.cesarsants.exceptions.MaisDeUmRegistroException;
import br.com.cesarsants.exceptions.TableException;
import br.com.cesarsants.exceptions.TipoChaveNaoEncontradaException;
import br.com.cesarsants.service.EntityManagerFactoryService;

import javax.persistence.NoResultException;
import java.util.List;

/**
 * @author cesarsants
 *
 */
public class UsuarioDAO extends GenericDAO<Usuario, Long> implements IUsuarioDAO {

	private static EntityManagerFactory getEmf() {
		return EntityManagerFactoryService.getEntityManagerFactory();
	}

	public UsuarioDAO() {
		super(Usuario.class);
		System.out.println("=== USUARIO DAO CONSTRUÍDO ===");
	}

	@Override
	public Usuario buscarPorEmail(String email) {
		System.out.println("=== DAO: BUSCANDO POR EMAIL ===");
		System.out.println("Email: " + email);
		
		EntityManager em = getEmf().createEntityManager();
		try {
			TypedQuery<Usuario> query = em.createQuery("SELECT u FROM Usuario u WHERE u.email = :email", Usuario.class);
			query.setParameter("email", email);
			List<Usuario> usuarios = query.getResultList();
			Usuario usuario = usuarios.isEmpty() ? null : usuarios.get(0);			if (usuario != null) {
				System.out.println("Usuário encontrado no banco: " + usuario.getNome());
			} else {
				System.out.println("Nenhum usuário encontrado com este email");
			}
			return usuario;
		} finally {
			em.close();
		}
	}

	@Override
	public Boolean existePorEmail(String email) {
		System.out.println("=== DAO: VERIFICANDO SE EMAIL EXISTE ===");
		System.out.println("Email: " + email);
		
		EntityManager em = getEmf().createEntityManager();
		try {
			TypedQuery<Long> query = em.createQuery("SELECT COUNT(u) FROM Usuario u WHERE u.email = :email", Long.class);
			query.setParameter("email", email);
			List<Long> counts = query.getResultList();
			Long count = counts.isEmpty() ? 0L : counts.get(0);			System.out.println("Quantidade de usuários com este email: " + count);
			return count > 0;
		} finally {
			em.close();
		}
	}

	public Usuario findByLogin(String login) throws DAOException {
		EntityManager em = getEmf().createEntityManager();
		try {
			TypedQuery<Usuario> query = em.createQuery("SELECT u FROM Usuario u WHERE u.login = :login", Usuario.class);
			query.setParameter("login", login);
			List<Usuario> result = query.getResultList();
			return result.isEmpty() ? null : result.get(0);
		} finally {
			em.close();
		}
	}

	public Usuario findByEmail(String email) throws DAOException {
		EntityManager em = getEmf().createEntityManager();
		try {
			TypedQuery<Usuario> query = em.createQuery("SELECT u FROM Usuario u WHERE u.email = :email", Usuario.class);
			query.setParameter("email", email);
			List<Usuario> result = query.getResultList();
			return result.isEmpty() ? null : result.get(0);
		} finally {
			em.close();
		}
	}

	public boolean existsByEmail(String email) throws DAOException {
		EntityManager em = getEmf().createEntityManager();
		try {
			TypedQuery<Long> query = em.createQuery("SELECT COUNT(u) FROM Usuario u WHERE u.email = :email", Long.class);
			query.setParameter("email", email);
			Long count = query.getSingleResult();
			return count > 0;
		} finally {
			em.close();
		}
	}
} 