package br.com.cesarsants.service;

import java.io.Serializable;
import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import br.com.cesarsants.dao.ISalaDAO;
import br.com.cesarsants.domain.Sala;
import br.com.cesarsants.services.generic.GenericService;

/**
 * @author cesarsants
 *
 */

@ApplicationScoped
public class SalaService extends GenericService<Sala, Long> implements ISalaService, Serializable {

    private static final long serialVersionUID = 1L;

    private ISalaDAO salaDAO;

    @Inject
    public SalaService(ISalaDAO salaDAO) {
        super(salaDAO);
        this.salaDAO = salaDAO;
    }

    @Override
    public Collection<Sala> buscarPorClinica(Long clinicaId) {
        return salaDAO.buscarPorClinica(clinicaId);
    }
} 