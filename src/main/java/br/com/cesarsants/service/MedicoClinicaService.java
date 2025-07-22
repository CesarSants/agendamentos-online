package br.com.cesarsants.service;

import java.util.List;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import br.com.cesarsants.dao.IMedicoClinicaDAO;
import br.com.cesarsants.domain.MedicoClinica;
import br.com.cesarsants.domain.Usuario;
import br.com.cesarsants.exceptions.DAOException;
import br.com.cesarsants.services.generic.GenericService;

public class MedicoClinicaService extends GenericService<MedicoClinica, Long> implements IMedicoClinicaService {
    
    private IMedicoClinicaDAO medicoClinicaDAO;
    
    public MedicoClinicaService() {
        this(new br.com.cesarsants.dao.MedicoClinicaDAO());
    }

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

    public IMedicoClinicaDAO getDao() {
        return medicoClinicaDAO;
    }

    /**
     * Remove o vínculo entre médico e clínica
     */
    public void removerVinculo(Long clinicaId, Long medicoId) throws DAOException {
        medicoClinicaDAO.removerVinculo(clinicaId, medicoId);
    }
}
