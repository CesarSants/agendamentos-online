/**
 * 
 */
package br.com.cesarsants.dao.generic;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import br.com.cesarsants.domain.Persistente;
import br.com.cesarsants.exceptions.DAOException;
import br.com.cesarsants.exceptions.MaisDeUmRegistroException;
import br.com.cesarsants.exceptions.TableException;
import br.com.cesarsants.exceptions.TipoChaveNaoEncontradaException;
import br.com.cesarsants.service.EntityManagerFactoryService;

/**
 * @author cesarsants
 *
 */
public class GenericDAO <T extends Persistente, E extends Serializable> implements IGenericDAO <T,E> {

    private static EntityManagerFactory getEmf() {
        return EntityManagerFactoryService.getEntityManagerFactory();
    }
    protected Class<T> persistenteClass;
    
    public GenericDAO(Class<T> persistenteClass) {
        this.persistenteClass = persistenteClass;
    }
    
    @Override
    public T cadastrar(T entity) throws TipoChaveNaoEncontradaException, DAOException {
        EntityManager entityManager = getEmf().createEntityManager();
        try {
            entityManager.getTransaction().begin();
            entityManager.persist(entity);
            entityManager.getTransaction().commit();
            return entity;
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw e;
        } finally {
            entityManager.close();
        }
    }

    @Override
    public void excluir(T entity) throws DAOException {
        EntityManager entityManager = getEmf().createEntityManager();
        try {
            entityManager.getTransaction().begin();
            if (entityManager.contains(entity)) {
                entityManager.remove(entity);
            } else {
                T managedCustomer = entityManager.find(this.persistenteClass, entity.getId());
                if (managedCustomer != null) {
                    entityManager.remove(managedCustomer);
                }
            }
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw e;
        } finally {
            entityManager.close();
        }
    }

    @Override
    public T alterar(T entity) throws TipoChaveNaoEncontradaException, DAOException {
        EntityManager entityManager = getEmf().createEntityManager();
        try {
            entityManager.getTransaction().begin();
            entity = entityManager.merge(entity);
            entityManager.flush();
            entityManager.getTransaction().commit();
            return entity;
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw e;
        } finally {
            entityManager.close();
        }
    }

    @Override
    public T consultar(E valor) throws MaisDeUmRegistroException, TableException, DAOException {
        EntityManager entityManager = getEmf().createEntityManager();
        try {
            T entity = entityManager.find(this.persistenteClass, valor);
            return entity;
        } finally {
            entityManager.close();
        }
    }

    @Override
    public Collection<T> buscarTodos() throws DAOException {
        EntityManager entityManager = getEmf().createEntityManager();
        try {
            List<T> list = 
                entityManager.createQuery(getSelectSql(), this.persistenteClass).getResultList();
            return list;
        } finally {
            entityManager.close();
        }
    }
    
    private String getSelectSql() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT obj FROM ");
        sb.append(this.persistenteClass.getSimpleName());
        sb.append(" obj");
        return sb.toString();
    }
}
