package br.com.cesarsants.service;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.servlet.http.HttpSession;

import br.com.cesarsants.dao.IPacienteDAO;
import br.com.cesarsants.domain.Paciente;
import br.com.cesarsants.domain.Usuario;
import br.com.cesarsants.exceptions.DAOException;
import br.com.cesarsants.services.generic.GenericService;

/**
 * @author cesarsants
 *
 */

@ApplicationScoped
public class PacienteService extends GenericService<Paciente, Long> implements IPacienteService {
    
    private IPacienteDAO pacienteDAO;
    
    @Inject
    public PacienteService(IPacienteDAO pacienteDAO) {
        super(pacienteDAO);
        this.pacienteDAO = pacienteDAO;
    }

    @Override
    public Paciente buscarPorCPF(Long cpf, Usuario usuario) throws DAOException {
        return this.pacienteDAO.buscarPorCPF(cpf, usuario);
    }

    @Override
    public List<Paciente> filtrarPacientes(String query) {
        Usuario usuarioLogado = getUsuarioLogado();
        return pacienteDAO.filtrarPacientes(query, usuarioLogado);
    }
    
    @Override
    public List<Paciente> buscarPorClinica(Long clinicaId) {
        Usuario usuarioLogado = getUsuarioLogado();
        return pacienteDAO.buscarPorClinica(clinicaId, usuarioLogado);
    }
    
    @Override
    public Paciente buscarPorNomeExato(String nome, Usuario usuario) {
        return pacienteDAO.buscarPorNomeExato(nome, usuario);
    }
    
    @Override
    public List<Paciente> buscarTodos() {
        Usuario usuarioLogado = getUsuarioLogado();
        return pacienteDAO.buscarTodos(usuarioLogado);
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