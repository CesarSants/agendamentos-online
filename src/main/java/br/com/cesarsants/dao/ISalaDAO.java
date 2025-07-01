package br.com.cesarsants.dao;

import java.util.Collection;

import br.com.cesarsants.dao.generic.IGenericDAO;
import br.com.cesarsants.domain.Sala;

/**
 * @author cesarsants
 *
 */

public interface ISalaDAO extends IGenericDAO<Sala, Long> {
    Collection<Sala> buscarPorClinica(Long clinicaId);
} 