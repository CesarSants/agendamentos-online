package br.com.cesarsants.service;

import java.util.List;
import java.util.stream.Collectors;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import br.com.cesarsants.dao.IMedicoDAO;
import br.com.cesarsants.domain.Clinica;
import br.com.cesarsants.domain.Medico;
import br.com.cesarsants.domain.Usuario;
import br.com.cesarsants.exceptions.DAOException;
import br.com.cesarsants.services.generic.GenericService;

public class MedicoService extends GenericService<Medico, Long> implements IMedicoService {
    private final IMedicoDAO medicoDAO;
    private IClinicaService clinicaService;

    public MedicoService() {
        this(new br.com.cesarsants.dao.MedicoDAO(), null);
    }

    public MedicoService(IMedicoDAO medicoDAO, IClinicaService clinicaService) {
        super(medicoDAO);
        this.medicoDAO = medicoDAO;
        this.clinicaService = clinicaService;
    }

    private IClinicaService getClinicaService() {
        if (clinicaService == null) {
            clinicaService = new br.com.cesarsants.service.ClinicaService();
        }
        return clinicaService;
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

    @Override
    public void excluir(Medico medico) throws DAOException {
        if (medico == null || medico.getId() == null) {
            throw new DAOException("Médico inválido para exclusão", null);
        }

        try {
            // Verifica se o médico tem agendamentos
            IAgendaService agendaService = new br.com.cesarsants.service.AgendaService();
            if (agendaService.medicoTemAgendamentos(medico.getId())) {
                throw new DAOException("Não é possível excluir o médico '" + medico.getNome() + "' pois ele possui agendamentos registrados no sistema.", null);
            }
            
            // Busca todas as clínicas vinculadas ao médico
            List<Clinica> clinicasVinculadas = getClinicaService().buscarPorMedico(medico.getId());

            // Se o médico estiver vinculado a alguma clínica, lança uma exceção com mensagem amigável
            if (!clinicasVinculadas.isEmpty()) {
                String nomesClinicas = clinicasVinculadas.stream()
                    .map(Clinica::getNome)
                    .collect(Collectors.joining(", "));
                
                throw new DAOException("Não é possível excluir o médico '" + medico.getNome() + "' pois ele está vinculado às seguintes clínicas: " + nomesClinicas, null);
            }

            // Se não houver vínculo nem agendamentos, prossegue com a exclusão
            super.excluir(medico);
        } catch (DAOException e) {
            // Propaga a exceção mantendo a mensagem original
            throw e;
        } catch (Exception e) {
            e.printStackTrace(); // Log do erro para debug
            throw new DAOException("Erro ao excluir médico: " + e.getMessage(), e);
        }
    }

    public Medico buscarPorCPF(Long cpf, Usuario usuario) throws DAOException {
        return this.medicoDAO.buscarPorCPF(cpf, usuario);
    }

    public Medico buscarPorNomeExato(String nome, Usuario usuario) {
        return medicoDAO.buscarPorNomeExato(nome, usuario);
    }

    @Override
    public List<Medico> filtrarMedicos(String query) {
        Usuario usuario = getUsuarioLogado();
        return medicoDAO.filtrarMedicos(query, usuario);
    }

    @Override
    public List<Medico> buscarPorClinica(Long clinicaId) {
        Usuario usuario = getUsuarioLogado();
        return medicoDAO.buscarPorClinica(clinicaId, usuario);
    }

    @Override
    public List<Medico> buscarTodos() {
        Usuario usuario = getUsuarioLogado();
        return medicoDAO.buscarTodos(usuario);
    }
}