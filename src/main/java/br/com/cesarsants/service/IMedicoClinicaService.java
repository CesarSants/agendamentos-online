package br.com.cesarsants.service;

import java.util.List;

import br.com.cesarsants.domain.MedicoClinica;
import br.com.cesarsants.services.generic.IGenericService;

public interface IMedicoClinicaService extends IGenericService<MedicoClinica, Long> {
    List<MedicoClinica> buscarPorClinica(Long clinicaId);
    List<MedicoClinica> buscarPorMedico(Long medicoId);
}
