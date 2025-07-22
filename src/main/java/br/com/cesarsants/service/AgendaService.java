package br.com.cesarsants.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import br.com.cesarsants.dao.IAgendaDAO;
import br.com.cesarsants.domain.Agenda;
import br.com.cesarsants.domain.Clinica;
import br.com.cesarsants.domain.Medico;
import br.com.cesarsants.domain.Paciente;
import br.com.cesarsants.domain.Usuario;
import br.com.cesarsants.exceptions.BusinessException;
import br.com.cesarsants.exceptions.DAOException;
import br.com.cesarsants.exceptions.TipoChaveNaoEncontradaException;
import br.com.cesarsants.services.generic.GenericService;

public class AgendaService extends GenericService<Agenda, Long> implements IAgendaService {
    
    private final IAgendaDAO agendaDAO;
    private final IPacienteService pacienteService;
    private final IMedicoService medicoService;
    private final IClinicaService clinicaService;
    
    // Instância singleton manual do AutoCompletionSessionManager
    private static final AutoCompletionSessionManager sessionManager = AutoCompletionSessionManager.getInstance();
    
    public AgendaService() {
        this(new br.com.cesarsants.dao.AgendaDAO(),
             new br.com.cesarsants.service.PacienteService(),
             new br.com.cesarsants.service.MedicoService(),
             new br.com.cesarsants.service.ClinicaService());
    }

    public AgendaService(IAgendaDAO agendaDAO, 
                        IPacienteService pacienteService,
                        IMedicoService medicoService,
                        IClinicaService clinicaService) {
        super(agendaDAO);
        this.agendaDAO = agendaDAO;
        this.pacienteService = pacienteService;
        this.medicoService = medicoService;
        this.clinicaService = clinicaService;
    }

    @Override
    public void concluirAgendamento(Agenda agenda) 
            throws TipoChaveNaoEncontradaException, DAOException {
        agendaDAO.concluirAgendamento(agenda);
    }

    @Override
    public void cancelarAgendamento(Agenda agenda) throws TipoChaveNaoEncontradaException, DAOException {
        if (!agenda.getStatus().equals(Agenda.Status.AGENDADO)) {
            throw new DAOException("Apenas consultas agendadas podem ser canceladas", new Exception());
        }
        
        agenda.setStatus(Agenda.Status.CANCELADO);
        agendaDAO.alterar(agenda);
    }

    @Override
    public Agenda criarAgendamento(Long pacienteId, Long medicoId, Long clinicaId,
                                 LocalDateTime dataHora, Integer numeroSala, String observacoes) 
            throws BusinessException {
        try {
            if (!verificarDisponibilidade(pacienteId, medicoId, clinicaId, dataHora, numeroSala)) {
                throw new BusinessException("Horário não disponível para agendamento");
            }

            Paciente paciente;
            Medico medico;
            Clinica clinica;
            try {
                paciente = pacienteService.consultar(pacienteId);
                medico = medicoService.consultar(medicoId);
                clinica = clinicaService.consultar(clinicaId);
            } catch (br.com.cesarsants.exceptions.MaisDeUmRegistroException | br.com.cesarsants.exceptions.DAOException | br.com.cesarsants.exceptions.TableException e) {
                throw new BusinessException("Erro ao consultar entidades: " + e.getMessage(), e);
            }

            // Criar novo agendamento
            Agenda agenda = new Agenda();
            agenda.setPaciente(paciente);
            agenda.setMedico(medico);
            agenda.setClinica(clinica);
            agenda.setDataHora(dataHora);
            agenda.setDataHoraFim(dataHora.plusMinutes(medico.getTempoConsulta()));
            agenda.setNumeroSala(numeroSala);
            agenda.setObservacoes(observacoes);
            agenda.setStatus(Agenda.Status.AGENDADO);
            
            // Associa o usuário logado ao agendamento
            Usuario usuarioLogado = getUsuarioLogado();
            if (usuarioLogado == null) {
                throw new BusinessException("Usuário não autenticado");
            }
            agenda.setUsuario(usuarioLogado);

            try {
                return cadastrar(agenda);
            } catch (Exception e) {
                throw new BusinessException("Erro ao salvar agendamento: " + e.getMessage(), e);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Erro ao criar agendamento: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Agenda> buscarAgendamentosPorPaciente(Long pacienteId) {
        Usuario usuarioLogado = getUsuarioLogado();
        return agendaDAO.buscarAgendamentosPorPaciente(pacienteId, usuarioLogado);
    }

    @Override
    public List<Agenda> buscarAgendamentosPorMedico(Long medicoId) {
        Usuario usuarioLogado = getUsuarioLogado();
        return agendaDAO.buscarAgendamentosPorMedico(medicoId, usuarioLogado);
    }

    @Override
    public List<Agenda> buscarAgendamentosPorClinica(Long clinicaId) {
        Usuario usuarioLogado = getUsuarioLogado();
        return agendaDAO.buscarAgendamentosPorClinica(clinicaId, usuarioLogado);
    }

    @Override
    public boolean verificarDisponibilidade(Long pacienteId, Long medicoId, Long clinicaId,
                                          LocalDateTime dataHora, Integer numeroSala) 
            throws BusinessException {
        try {
            Medico medico;
            try {
                medico = medicoService.consultar(medicoId);
            } catch (br.com.cesarsants.exceptions.MaisDeUmRegistroException | br.com.cesarsants.exceptions.DAOException | br.com.cesarsants.exceptions.TableException e) {
                throw new BusinessException("Erro ao consultar médico: " + e.getMessage(), e);
            }

            // Verifica horário de funcionamento da clínica
            if (!clinicaService.verificarHorarioFuncionamento(clinicaId, dataHora)) {
                throw new BusinessException("Clínica fechada neste horário");
            }

            // Verifica disponibilidade do médico
            Usuario usuarioLogado = getUsuarioLogado();
            if (!agendaDAO.isMedicoDisponivel(medicoId, dataHora, medico.getTempoConsulta(), usuarioLogado)) {
                throw new BusinessException("Médico não disponível neste horário");
            }
            // Verifica disponibilidade do paciente
            if (!agendaDAO.isPacienteDisponivel(pacienteId, dataHora, medico.getTempoConsulta(), usuarioLogado)) {
                throw new BusinessException("Paciente já possui agendamento neste horário");
            }

            // Verifica disponibilidade da sala
            if (!clinicaService.verificarDisponibilidadeSala(clinicaId, numeroSala, 
                    dataHora, medico.getTempoConsulta())) {
                throw new BusinessException("Sala não disponível neste horário");
            }

            return true;
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new BusinessException("Erro ao verificar disponibilidade: " + e.getMessage(), e);
        }
    }

    @Override
    public void excluirAgendamento(Long agendamentoId) throws BusinessException {
        try {
            System.out.println("AgendaService: Iniciando exclusão do agendamento ID: " + agendamentoId);
            
            // Verifica se o agendamento existe antes de tentar excluir
            Agenda agenda = this.consultar(agendamentoId);
            if (agenda == null) {
                throw new BusinessException("Agendamento não encontrado", new Exception());
            }
            
            System.out.println("AgendaService: Agendamento encontrado - Paciente: " + agenda.getPaciente().getNome() + 
                             ", Médico: " + agenda.getMedico().getNome() + ", Data: " + agenda.getDataHora());
            
            agendaDAO.excluirAgendamento(agendamentoId);
            
            System.out.println("AgendaService: Agendamento excluído com sucesso");
            
        } catch (BusinessException e) {
            System.err.println("AgendaService: BusinessException ao excluir agendamento: " + e.getMessage());
            throw e;
        } catch (DAOException | TipoChaveNaoEncontradaException e) {
            System.err.println("AgendaService: DAOException/TipoChaveNaoEncontradaException ao excluir agendamento: " + e.getMessage());
            throw new BusinessException("Erro ao excluir agendamento: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("AgendaService: Exception geral ao excluir agendamento: " + e.getMessage());
            e.printStackTrace();
            throw new BusinessException("Erro inesperado ao excluir agendamento: " + e.getMessage(), e);
        }
    }

    @Override
    public void finalizarAgendamento(Long agendamentoId) throws BusinessException {
        try {
            // Busca o agendamento
            Agenda agenda = this.consultar(agendamentoId);
            if (agenda == null) {
                throw new BusinessException("Agendamento não encontrado", new Exception());
            }
            
            // Verifica se pode ser finalizado
            if (!agenda.getStatus().equals(Agenda.Status.AGENDADO)) {
                throw new BusinessException("Apenas consultas agendadas podem ser finalizadas", new Exception());
            }
            
            // Atualiza o status e o horário final
            agenda.setStatus(Agenda.Status.CONCLUIDO);
            agenda.setDataHoraFim(LocalDateTime.now());
            agendaDAO.alterar(agenda);
            
        } catch (DAOException | TipoChaveNaoEncontradaException | br.com.cesarsants.exceptions.MaisDeUmRegistroException | br.com.cesarsants.exceptions.TableException e) {
            throw new BusinessException("Erro ao finalizar agendamento: " + e.getMessage(), e);
        }
    }

    @Override
    public Collection<Agenda> buscarPorSalaEData(Long salaId, LocalDate data) {
        Usuario usuarioLogado = getUsuarioLogado();
        return agendaDAO.buscarPorSalaEData(salaId, data, usuarioLogado);
    }

    @Override
    public Collection<Agenda> buscarTodosComRelacionamentos() throws BusinessException {
        try {
            Usuario usuarioLogado = getUsuarioLogado();
            return agendaDAO.buscarTodosComRelacionamentos(usuarioLogado);
        } catch (DAOException e) {
            throw new BusinessException("Erro ao buscar agendamentos com relacionamentos: " + e.getMessage(), e);
        }
    }

    @Override
    public void concluirAgendamentosAutomaticamente() throws BusinessException {
        try {
            Collection<Agenda> agendamentosParaConcluir = buscarAgendamentosParaConclusaoAutomatica();
            
            for (Agenda agenda : agendamentosParaConcluir) {
                try {
                    concluirAgendamentoAutomatico(agenda);
                } catch (Exception e) {
                    // Log do erro mas continua processando outros agendamentos
                    System.err.println("Erro ao concluir automaticamente agendamento ID " + agenda.getId() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new BusinessException("Erro ao concluir agendamentos automaticamente: " + e.getMessage(), e);
        }
    }

    @Override
    public Collection<Agenda> buscarAgendamentosParaConclusaoAutomatica() throws BusinessException {
        try {
            // Verifica se há usuários ativos
            if (!sessionManager.hasUsuariosAtivos()) {
                System.out.println("Nenhum usuário com sistema de atualização automática ativo. " +
                                 "Retornando lista vazia de agendamentos para conclusão automática.");
                return List.of();
            }
            
            Set<Long> usuariosAtivos = sessionManager.getUsuariosAtivos();
            LocalDateTime agora = LocalDateTime.now();
            
            System.out.println("=== BUSCANDO AGENDAMENTOS PARA CONCLUSÃO AUTOMÁTICA ===");
            System.out.println("Horário atual: " + agora);
            System.out.println("Usuários ativos: " + usuariosAtivos.size() + " - IDs: " + usuariosAtivos);
            
            // Busca agendamentos de todos os usuários ativos
            Collection<Agenda> todosAgendamentos = new ArrayList<>();
            for (Long usuarioId : usuariosAtivos) {
                try {
                    // Simula o contexto do usuário para buscar seus agendamentos
                    Collection<Agenda> agendamentosUsuario = buscarAgendamentosPorUsuario(usuarioId);
                    todosAgendamentos.addAll(agendamentosUsuario);
                } catch (Exception e) {
                    System.err.println("Erro ao buscar agendamentos do usuário " + usuarioId + ": " + e.getMessage());
                }
            }
            
            System.out.println("Total de agendamentos dos usuários ativos: " + todosAgendamentos.size());
            
            Collection<Agenda> agendamentosFiltrados = todosAgendamentos.stream()
                .filter(agenda -> {
                    boolean statusAgendado = Agenda.Status.AGENDADO.equals(agenda.getStatus());
                    boolean horarioPassou = agenda.getDataHoraFim() != null && agenda.getDataHoraFim().isBefore(agora);
                    
                    System.out.println("Agendamento ID " + agenda.getId() + 
                                     " | Usuário: " + agenda.getUsuario().getId() +
                                     " | Status: " + agenda.getStatus() + 
                                     " | DataHoraFim: " + agenda.getDataHoraFim() + 
                                     " | Status Agendado: " + statusAgendado + 
                                     " | Horário Passou: " + horarioPassou + 
                                     " | Incluir: " + (statusAgendado && horarioPassou));
                    
                    return statusAgendado && horarioPassou;
                })
                .collect(Collectors.toList());
            
            System.out.println("Agendamentos filtrados para conclusão automática: " + agendamentosFiltrados.size());
            System.out.println("=== FIM DA BUSCA ===");
            
            return agendamentosFiltrados;
        } catch (Exception e) {
            throw new BusinessException("Erro ao buscar agendamentos para conclusão automática: " + e.getMessage(), e);
        }
    }
    
    /**
     * Busca agendamentos de um usuário específico
     * @param usuarioId ID do usuário
     * @return Collection de agendamentos do usuário
     */
    private Collection<Agenda> buscarAgendamentosPorUsuario(Long usuarioId) throws DAOException {
        return agendaDAO.buscarTodosComRelacionamentosPorUsuarioId(usuarioId);
    }

    @Override
    public void concluirAgendamentoAutomatico(Agenda agenda) throws BusinessException {
        try {
            // Verifica se o agendamento ainda está com status AGENDADO
            if (!Agenda.Status.AGENDADO.equals(agenda.getStatus())) {
                return; // Se já foi alterado manualmente, não faz nada
            }
            
            // Atualiza o status para CONCLUIDO
            agenda.setStatus(Agenda.Status.CONCLUIDO);
            
            // Salva a alteração
            agendaDAO.alterar(agenda);
            
            System.out.println("Agendamento ID " + agenda.getId() + " do usuário " + 
                             agenda.getUsuario().getId() + " concluído automaticamente às " + LocalDateTime.now());
            
        } catch (DAOException | TipoChaveNaoEncontradaException e) {
            throw new BusinessException("Erro ao concluir agendamento automaticamente: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean salaTemAgendamentos(Long salaId) {
        Usuario usuarioLogado = getUsuarioLogado();
        return agendaDAO.salaTemAgendamentos(salaId, usuarioLogado);
    }
    
    @Override
    public boolean medicoTemAgendamentos(Long medicoId) {
        Usuario usuarioLogado = getUsuarioLogado();
        return agendaDAO.medicoTemAgendamentos(medicoId, usuarioLogado);
    }
    
    @Override
    public boolean pacienteTemAgendamentos(Long pacienteId) {
        Usuario usuarioLogado = getUsuarioLogado();
        return agendaDAO.pacienteTemAgendamentos(pacienteId, usuarioLogado);
    }
    
    @Override
    public boolean clinicaTemAgendamentos(Long clinicaId) {
        Usuario usuarioLogado = getUsuarioLogado();
        return agendaDAO.clinicaTemAgendamentos(clinicaId, usuarioLogado);
    }
    
    @Override
    public List<Agenda> buscarTodos() {
        Usuario usuarioLogado = getUsuarioLogado();
        return agendaDAO.buscarTodos(usuarioLogado);
    }
    
    private Usuario getUsuarioLogado() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            // Proteção: nunca deve ser chamado em thread de background
            throw new IllegalStateException("getUsuarioLogado() chamado fora de contexto JSF! Não use este método em schedulers ou threads de background.");
        }
        HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(false);
        if (session != null) {
            return (Usuario) session.getAttribute("usuarioLogado");
        }
        return null;
    }
}