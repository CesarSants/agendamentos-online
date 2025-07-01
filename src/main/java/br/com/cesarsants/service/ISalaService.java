package br.com.cesarsants.service;

import java.util.Collection;

import br.com.cesarsants.domain.Sala;
import br.com.cesarsants.services.generic.IGenericService;

/**
 * @author cesarsants
 *
 */

public interface ISalaService extends IGenericService<Sala, Long> {
    Collection<Sala> buscarPorClinica(Long clinicaId);
} 