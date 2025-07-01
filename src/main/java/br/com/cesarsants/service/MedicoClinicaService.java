package br.com.cesarsants.service;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.servlet.http.HttpSession;

import br.com.cesarsants.dao.IMedicoClinicaDAO;
import br.com.cesarsants.domain.MedicoClinica;
import br.com.cesarsants.domain.Usuario;
import br.com.cesarsants.services.generic.GenericService;

/**
 * @author cesarsants
 *
 */
		
@ApplicationScoped
public class MedicoClinicaService extends GenericService<MedicoClinica, Long> implements IMedicoClinicaService {
    
    private IMedicoClinicaDAO medicoClinicaDAO;
    
    @Inject
    public MedicoClinicaService(IMedicoClinicaDAO medicoClinicaDAO) {
        super(medicoClinicaDAO);
        this.medicoClinicaDAO = medicoClinicaDAO;
    }

    @Override
    public List<MedicoClinica> buscarPorClinica(Long clinicaId) {
        Usuario usuarioLogado = getUsuarioLogado();
        return medicoClinicaDAO.buscarPorClinica(clinicaId, usuarioLogado);
    }

    @Override
    public List<MedicoClinica> buscarPorMedico(Long medicoId) {
        Usuario usuarioLogado = getUsuarioLogado();
        return medicoClinicaDAO.buscarPorMedico(medicoId, usuarioLogado);
    }
    
    private Usuario getUsuarioLogado() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(false);
            if (session != null) {
                return (Usuario) session.getAttribute("usuarioLogado");
            }
        }
        return null;
    }
}
