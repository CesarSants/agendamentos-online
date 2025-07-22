package br.com.cesarsants.service;

import java.util.List;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import br.com.cesarsants.dao.IPacienteDAO;
import br.com.cesarsants.domain.Paciente;
import br.com.cesarsants.domain.Usuario;
import br.com.cesarsants.exceptions.DAOException;
import br.com.cesarsants.services.generic.GenericService;

public class PacienteService extends GenericService<Paciente, Long> implements IPacienteService {
    
    private IPacienteDAO pacienteDAO;

    public PacienteService() {
        this(new br.com.cesarsants.dao.PacienteDAO());
    }

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
    
    @Override
    public void excluir(Paciente paciente) throws DAOException {
        if (paciente == null || paciente.getId() == null) {
            throw new DAOException("Paciente inválido para exclusão", null);
        }

        try {
            // Verifica se o paciente tem agendamentos
            IAgendaService agendaService = new br.com.cesarsants.service.AgendaService();
            if (agendaService.pacienteTemAgendamentos(paciente.getId())) {
                throw new DAOException("Não é possível excluir o paciente '" + paciente.getNome() + "' pois ele possui agendamentos registrados no sistema.", null);
            }

            // Se não houver agendamentos, prossegue com a exclusão
            super.excluir(paciente);
        } catch (DAOException e) {
            // Propaga a exceção mantendo a mensagem original
            throw e;
        } catch (Exception e) {
            e.printStackTrace(); // Log do erro para debug
            throw new DAOException("Erro ao excluir paciente: " + e.getMessage(), e);
        }
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