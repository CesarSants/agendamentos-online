package br.com.cesarsants.dao;

import java.util.List;

import br.com.cesarsants.dao.generic.IGenericDAO;
import br.com.cesarsants.domain.MedicoClinica;
import br.com.cesarsants.domain.Usuario;

/**
 * @author cesarsants
 *
 */

public interface IMedicoClinicaDAO extends IGenericDAO<MedicoClinica, Long> {
    List<MedicoClinica> buscarPorClinica(Long clinicaId, Usuario usuario);
    List<MedicoClinica> buscarPorMedico(Long medicoId, Usuario usuario);
}
