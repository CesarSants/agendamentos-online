package br.com.cesarsants.controller;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;

import br.com.cesarsants.domain.Agenda;
import br.com.cesarsants.domain.Clinica;
import br.com.cesarsants.domain.Medico;
import br.com.cesarsants.domain.Paciente;
import br.com.cesarsants.domain.Sala;
import br.com.cesarsants.domain.Usuario;
import br.com.cesarsants.exceptions.BusinessException;
import br.com.cesarsants.service.AgendaAutoCompletionScheduler;
import br.com.cesarsants.service.AutoCompletionSessionManager;
import br.com.cesarsants.service.IAgendaService;
import br.com.cesarsants.service.IClinicaService;
import br.com.cesarsants.service.IMedicoService;
import br.com.cesarsants.service.IPacienteService;
import br.com.cesarsants.service.ISalaService;
import br.com.cesarsants.service.IUsuarioService;

/**
 * @author cesarsants
 *
 */

@Named
@ViewScoped
public class AgendaController implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Collection<Agenda> agendamentos;
    private Collection<Clinica> clinicas;
    private Collection<Medico> medicos;
    private Collection<Paciente> pacientes;

    private List<String> medicosIndisponiveisNomes;
    private List<String> pacientesIndisponiveisNomes;
    private List<String> salasIndisponiveisNomes;
    
    private Long selectedClinicaId;
    private Medico selectedMedico;
    private Sala selectedSala;
    private LocalDateTime selectedData;
    private LocalTime selectedHorario;
    private Paciente selectedPaciente;
    private Agenda selectedConsulta;
    private LocalDate selectedDate;
    private LocalTime selectedTime;

    private String observacoes;
    private List<Agenda> consultasAgendadas;
    private Boolean isUpdate;
    private Collection<Sala> salasDisponiveis;
    private Collection<Sala> salasIndisponiveis;
    private LocalDate today;

    private String dynamicSelectedPaciente;
    private String dynamicSelectedMedico;
    private String dynamicSelectedClinica;
    private String dynamicSelectedSala;
    private String dynamicDateTime;

    private Medico editMedico;
    private Sala editSala;
    private String editObservacoes;
    private LocalDateTime editDataHora;

    private String dynamicEditSala;
    private String dynamicEditDateTime;
    
    private List<Medico> medicosDisponiveisParaEdicao;
    private List<Sala> salasDisponiveisParaEdicao;

    private Medico medicoAtual;
    private Sala salaAtual;
    private String hashAgendamentosAtual;
    private boolean mudancaDetectada = false;

    private boolean sistemaAtualizacaoAtivo = true; 
    private ScheduledExecutorService scheduler;
    
    @Inject
    private AgendaAutoCompletionScheduler autoCompletionScheduler;
    
    @Inject
    private AutoCompletionSessionManager sessionManager;
    
    @Inject
    private IAgendaService agendaService;
    
    @Inject
    private IClinicaService clinicaService;
    
    @Inject
    private IMedicoService medicoService;
    
    @Inject
    private IPacienteService pacienteService;

    @Inject
    private ISalaService salaService;
    
    @Inject
    private IUsuarioService usuarioService;

    private String searchEntityType = "data";
    private String searchType = "nome"; 
    private String searchText;
    private LocalDate searchDate;
    private LocalTime searchTime;
    private Collection<Agenda> agendasFiltradas;
    
    private String ordenacao = "data_recente"; // data_recente, data_antiga, clinica, medico, paciente
    
    @PostConstruct
    public void init() {
        try {
            this.isUpdate = false;
            this.agendamentos = agendaService.buscarTodosComRelacionamentos();
            this.clinicas = clinicaService.buscarTodos();
            this.medicos = medicoService.buscarTodos();
            this.pacientes = pacienteService.buscarTodos();
            this.today = LocalDate.now();
            this.selectedDate = this.today;
            this.selectedTime = null;
            this.medicosIndisponiveisNomes = new ArrayList<>();
            this.pacientesIndisponiveisNomes = new ArrayList<>();
            this.salasIndisponiveisNomes = new ArrayList<>();
            this.dynamicDateTime = "";
            this.dynamicSelectedMedico = "";
            this.dynamicSelectedClinica = "";
            this.dynamicSelectedPaciente = "";
            this.dynamicSelectedSala = "";
            this.dynamicEditDateTime = "";
            this.medicosDisponiveisParaEdicao = new ArrayList<>();
            this.salasDisponiveisParaEdicao = new ArrayList<>();
            this.medicoAtual = null;
            this.salaAtual = null;
            
            this.hashAgendamentosAtual = gerarHashAgendamentos();
            this.mudancaDetectada = false;
            
            boolean estadoSalvo = isSistemaAtualizacaoAtivo();
            
            if (estadoSalvo) {
                // Obtém o usuário logado
                Usuario usuarioLogado = getUsuarioLogado();
                if (usuarioLogado != null) {
                	 if (usuarioLogado.isAtualizacaoAutomaticaAtiva()) {
                         sessionManager.adicionarUsuarioAtivo(usuarioLogado.getId());
                         scheduler = Executors.newSingleThreadScheduledExecutor();
                         scheduler.scheduleAtFixedRate(() -> {
                             verificarMudancasEAtualizar();
                         }, 0, 1, TimeUnit.MINUTES);
                     } else {
                         sessionManager.removerUsuarioAtivo(usuarioLogado.getId());
                     }
                 }
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao inicializar: " + e.getMessage()));
        }
    }    public void novoAgendamento() {
        this.selectedClinicaId = null;
        this.selectedMedico = null;
        this.selectedSala = null;
        this.selectedData = null;
        this.selectedTime = null;
        this.selectedPaciente = null;
        this.observacoes = null;
        this.isUpdate = false;

        this.dynamicDateTime = "";
        this.dynamicSelectedMedico = "";
        this.dynamicSelectedClinica = "";
        this.dynamicSelectedPaciente = "";
        this.dynamicSelectedSala = "";
        this.medicosIndisponiveisNomes = new ArrayList<>();
        this.pacientesIndisponiveisNomes = new ArrayList<>();
        this.salasIndisponiveisNomes = new ArrayList<>();
        
        if (FacesContext.getCurrentInstance() != null) {
            PrimeFaces.current().ajax().update(":datetimeForm:datetime-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:medico-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:clinica-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:medicos-indisponiveis-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:paciente-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:pacientes-indisponiveis-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:sala-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:salas-indisponiveis-panel");
        }
    }        

    private boolean verificarDisponibilidade() throws BusinessException {
        return agendaService.verificarDisponibilidade(
            this.selectedPaciente.getId(),
            this.selectedMedico.getId(),
            this.selectedClinicaId,
            this.selectedData,
            this.selectedSala.getOrdem()
        );
    }

    public Collection<Medico> getMedicosDaClinica() {
        if (selectedClinicaId == null) {
            return new ArrayList<>();
        }
        try {
            Collection<Medico> medicosClinica = medicoService.buscarPorClinica(selectedClinicaId);
            if (medicosClinica == null || medicosClinica.isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Info", "Não há médicos cadastrados para esta clínica"));
            }
            return medicosClinica != null ? medicosClinica : new ArrayList<>();
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro ao buscar médicos da clínica", e.getMessage()));
            return new ArrayList<>();
        }
    }

    public List<LocalTime> getHorariosDisponiveis() {
        if (selectedClinicaId == null || selectedData == null) {
            return new ArrayList<>();
        }
        try {
            Clinica clinica;
            try {
                clinica = clinicaService.consultar(selectedClinicaId);
            } catch (br.com.cesarsants.exceptions.MaisDeUmRegistroException | br.com.cesarsants.exceptions.TableException e) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro ao buscar horários disponíveis", e.getMessage()));
                return new ArrayList<>();
            }
            if (clinica == null) return new ArrayList<>();
            LocalTime abertura = clinica.getHorarioAbertura();
            LocalTime fechamento = clinica.getHorarioFechamento();
            if (abertura == null || fechamento == null) return new ArrayList<>();
            List<LocalTime> horariosPossiveis = java.util.stream.Stream.iterate(abertura, h -> h.plusHours(1))
                .limit(fechamento.toSecondOfDay() / 3600 - abertura.toSecondOfDay() / 3600 + 1)
                .collect(Collectors.toList());
            List<Agenda> agendamentosClinica = agendaService.buscarAgendamentosPorClinica(clinica.getId())
                .stream().filter(a -> a.getDataHora().toLocalDate().equals(selectedData.toLocalDate()))
                .collect(Collectors.toList());  
            Collection<Sala> salas = clinica.getSalas();
            if (salas == null || salas.isEmpty()) return new ArrayList<>();
            
            List<LocalTime> horariosDisponiveis = new ArrayList<>();
            for (LocalTime horario : horariosPossiveis) {
                long ocupadas = agendamentosClinica.stream()
                    .filter(a -> a.getDataHora().toLocalTime().equals(horario))
                    .count();
                if (ocupadas < salas.size()) {
                    boolean pacienteConflito = agendamentosClinica.stream()
                        .anyMatch(a -> a.getPaciente().getId().equals(selectedPaciente != null ? selectedPaciente.getId() : null)
                            && a.getDataHora().toLocalTime().equals(horario));
                    boolean medicoConflito = agendamentosClinica.stream()
                        .anyMatch(a -> a.getMedico().getId().equals(selectedMedico.getId())
                            && a.getDataHora().toLocalTime().equals(horario));
                    if (!pacienteConflito && !medicoConflito) {
                        horariosDisponiveis.add(horario);
                    }
                }
            }
            return horariosDisponiveis;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro ao buscar horários disponíveis", e.getMessage()));
            return new ArrayList<>();
        }
    }

    public List<Paciente> completePaciente(String query) {
        String queryLowerCase = query.toLowerCase();
        return pacientes.stream()
                .filter(p -> p.getNome().toLowerCase().contains(queryLowerCase))
                .collect(Collectors.toList());
    }    
    
    private void onClinicaChangeOld() {
        try {
            this.selectedSala = null;
            this.selectedData = null;
            this.selectedHorario = null;
            
            if (this.selectedClinicaId == null) {
                this.salasDisponiveis = new ArrayList<>();
                return;
            }

            Clinica clinica = clinicaService.consultar(this.selectedClinicaId);
            if (clinica == null) {
                this.salasDisponiveis = new ArrayList<>();
                return;
            }

            this.salasDisponiveis = clinica.getSalas().stream()
                .sorted(Comparator.comparing(Sala::getOrdem))
                .collect(Collectors.toList());

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao carregar dados da clínica: " + e.getMessage()));
            this.medicos = new ArrayList<>();
            this.salasDisponiveis = new ArrayList<>();
        }
    }

    public List<SelectItem> getSalaItems() {
        List<SelectItem> items = new ArrayList<>();
        if (selectedConsulta != null) {
            Collection<Sala> salasDisponiveis = getSalasDisponiveis();
            if (salasDisponiveis != null && !salasDisponiveis.isEmpty()) {
                salasDisponiveis.stream()
                    .sorted(Comparator.comparing(Sala::getOrdem))
                    .forEach(sala -> items.add(new SelectItem(sala.getOrdem(), "Sala " + sala.getOrdem())));
            }
        }
        return items;
    }

 private void refreshAgendamentos() {
        try {
            Collection<Agenda> agendamentos = agendaService.buscarTodosComRelacionamentos();
            Map<Long, Collection<Sala>> salasClinicaCache = new HashMap<>();
            
            for (Agenda agenda : agendamentos) {
                if (agenda.getSala() == null) {
                    try {
                        Collection<Sala> salasClinica = salasClinicaCache.computeIfAbsent(
                            agenda.getClinica().getId(),
                            clinicaId -> {
                                try {
                                    return salaService.buscarPorClinica(clinicaId);
                                } catch (Exception e) {
                                    System.err.println("Erro ao buscar salas da clínica " + clinicaId + ": " + e.getMessage());
                                    return Collections.emptyList();
                                }
                            }
                        );
                        
                        Sala sala = salasClinica.stream()
                            .filter(s -> s.getOrdem().equals(agenda.getNumeroSala()))
                            .findFirst()
                            .orElseThrow(() -> new BusinessException(
                                "Sala " + agenda.getNumeroSala() + " não encontrada na clínica " + agenda.getClinica().getNome()));
                        
                        agenda.setSala(sala);
                        
                    } catch (Exception ex) {
                        System.err.println("Erro ao carregar sala para agendamento " + agenda.getId() + 
                                         " (Clínica: " + agenda.getClinica().getId() + 
                                         ", Sala: " + agenda.getNumeroSala() + "): " + ex.getMessage());
                    }
                }
            }
            
            List<Agenda> agendamentosFiltrados = agendamentos.stream()
                .filter(a -> a.getSala() != null)
                .collect(Collectors.toList());
            
            this.consultasAgendadas = aplicarOrdenacao(agendamentosFiltrados);
                
        } catch (Exception e) {
            this.consultasAgendadas = new ArrayList<>();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao carregar agendamentos: " + e.getMessage()));
        }
    }

    public void salvarAgendamento() {
        FacesContext context = FacesContext.getCurrentInstance();
        
        try {
            if (selectedPaciente == null || selectedClinicaId == null || selectedSala == null || selectedMedico == null) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Todos os campos são obrigatórios"));
                context.validationFailed();
                return;
            }

            updateSelectedDateTime();
            if (selectedData == null) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Selecione uma data e horário"));
                context.validationFailed();
                return;
            }

            // Associa o usuário logado ao agendamento
            Usuario usuarioLogado = getUsuarioLogado();
            if (usuarioLogado == null) {
                throw new BusinessException("Usuário não autenticado");
            }

            Agenda novoAgendamento = agendaService.criarAgendamento(
                selectedPaciente.getId(),
                selectedMedico.getId(),
                selectedClinicaId,
                selectedData,
                selectedSala.getOrdem(),
                observacoes
            );
            
            novoAgendamento.setSala(selectedSala);
            
            refreshAgendamentos();
            
            novoAgendamento();
            this.dynamicDateTime = "";
            this.dynamicSelectedMedico = "";
            this.dynamicSelectedClinica = "";
            this.dynamicSelectedPaciente = "";
            this.dynamicSelectedSala = "";
            this.medicosIndisponiveisNomes = new ArrayList<>();
            this.pacientesIndisponiveisNomes = new ArrayList<>();
            this.salasIndisponiveisNomes = new ArrayList<>();

            PrimeFaces.current().ajax().update(":datetimeForm:datetime-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:medico-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:clinica-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:medicos-indisponiveis-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:paciente-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:pacientes-indisponiveis-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:sala-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:salas-indisponiveis-panel");
            
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, 
                "Sucesso", "Agendamento realizado com sucesso"));

        } catch (BusinessException e) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                "Erro de negócio", e.getMessage()));
            context.validationFailed();
        } catch (Exception e) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                "Erro", "Erro ao realizar agendamento: " + e.getMessage()));
            context.validationFailed();
        }
    }

    private Usuario getUsuarioLogado() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            javax.servlet.http.HttpSession session = (javax.servlet.http.HttpSession) facesContext.getExternalContext().getSession(false);
            if (session != null) {
                return (Usuario) session.getAttribute("usuarioLogado");
            }
        }
        return null;
    }

    public void finalizarConsulta(Agenda consulta) {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            consulta = agendaService.consultar(consulta.getId());
            
            if (!consulta.getStatus().equals(Agenda.Status.AGENDADO)) {
                throw new BusinessException("Apenas consultas agendadas podem ser finalizadas");
            }
            
            agendaService.finalizarAgendamento(consulta.getId());
            
            refreshAgendamentos();
            
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, 
                "Sucesso", "Consulta finalizada com sucesso"));
            verificarMedicosIndisponiveisEmTempoReal();
            verificarPacientesIndisponiveisEmTempoReal();
            verificarSalasIndisponiveisEmTempoReal();
            PrimeFaces.current().ajax().update(":datetimeForm:medico-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:paciente-panel");
            PrimeFaces.current().ajax().update("scheduleForm:medico");
            PrimeFaces.current().ajax().update("scheduleForm:paciente");
        } catch (BusinessException e) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                "Erro", "Erro ao finalizar consulta: " + e.getMessage()));
        } catch (Exception e) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                "Erro", "Erro inesperado ao finalizar consulta"));
        }
    }

    public void cancelarConsulta(Agenda consulta) {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            if (!consulta.getStatus().equals(Agenda.Status.AGENDADO)) {
                context.validationFailed();
                throw new BusinessException("Apenas consultas agendadas podem ser canceladas");
            }

            agendaService.cancelarAgendamento(consulta);
            
            refreshAgendamentos();
            
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, 
                "Sucesso", "Consulta cancelada com sucesso"));
            verificarMedicosIndisponiveisEmTempoReal();
            verificarPacientesIndisponiveisEmTempoReal();
            verificarSalasIndisponiveisEmTempoReal();
            PrimeFaces.current().ajax().update(":datetimeForm:medico-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:paciente-panel");
            PrimeFaces.current().ajax().update("scheduleForm:medico");
            PrimeFaces.current().ajax().update("scheduleForm:paciente");
        } catch (BusinessException e) {
            context.validationFailed();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                "Erro", "Erro ao cancelar consulta: " + e.getMessage()));
        } catch (Exception e) {
            context.validationFailed();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                "Erro", "Erro inesperado ao cancelar consulta"));
        }
    }   
    
    public void excluirConsulta(Agenda consulta) {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            agendaService.excluirAgendamento(consulta.getId());
            
            Collection<Agenda> currentAgendamentos = agendaService.buscarTodosComRelacionamentos();
            this.consultasAgendadas = new ArrayList<>(currentAgendamentos);
            this.consultasAgendadas.sort((a1, a2) -> a2.getDataHora().compareTo(a1.getDataHora()));
            
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, 
                "Sucesso", "Consulta excluída com sucesso"));
        } catch (BusinessException e) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                "Erro", "Erro ao excluir consulta: " + e.getMessage()));
        } catch (Exception e) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                "Erro", "Erro inesperado ao excluir consulta"));
        }
    }
    
     public void onDataChange() {
        try {
            String timeDisplay = "";
            String dateDisplay = "";
            boolean hasInvalidInput = false;
            
            UIInput timeInput = (UIInput) FacesContext.getCurrentInstance().getViewRoot()
                    .findComponent("scheduleForm:horario");
            if (timeInput != null && timeInput.getSubmittedValue() != null) {
                String rawTimeValue = timeInput.getSubmittedValue().toString().trim();
                try {
                    selectedTime = LocalTime.parse(rawTimeValue, DateTimeFormatter.ofPattern("HH:mm"));
                    timeDisplay = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm"));
                } catch (DateTimeParseException e) {
                    System.out.println("Invalid time format: " + rawTimeValue);
                    selectedTime = null;
                    hasInvalidInput = true;
                }
            }
            
            UIInput dateInput = (UIInput) FacesContext.getCurrentInstance().getViewRoot()
                    .findComponent("scheduleForm:dataSelecionada");
            if (dateInput != null && dateInput.getSubmittedValue() != null) {
                String rawDateValue = dateInput.getSubmittedValue().toString().trim();
                try {
                    selectedDate = LocalDate.parse(rawDateValue, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    dateDisplay = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                } catch (DateTimeParseException e) {
                    System.out.println("Invalid date format: " + rawDateValue);
                    selectedDate = null;
                    hasInvalidInput = true;
                }
            }
            
            if (hasInvalidInput) {
                this.dynamicDateTime = "";
                this.pacientesIndisponiveisNomes = new ArrayList<>();
            } else {
                StringBuilder displayBuilder = new StringBuilder();
                if (!dateDisplay.isEmpty()) {
                    displayBuilder.append(dateDisplay);
                }
                if (!timeDisplay.isEmpty()) {
                    if (displayBuilder.length() > 0) {
                        displayBuilder.append(" ");
                    }
                    displayBuilder.append(timeDisplay);
                }
                this.dynamicDateTime = displayBuilder.toString();
            }
            
            PrimeFaces.current().ajax().update(":datetimeForm:datetime-panel");
            
            verificarMedicosIndisponiveisEmTempoReal();
            verificarPacientesIndisponiveisEmTempoReal();
            verificarSalasIndisponiveisEmTempoReal();
            
            if (selectedDate != null && selectedTime != null) {
                updateSelectedDateTime();
                verificarDisponibilidadeMedicoEmTempoReal();
                verificarDisponibilidadePacienteEmTempoReal();
                verificarDisponibilidadeSalaEmTempoReal();
                if (selectedClinicaId != null) {
                    verificarDisponibilidadeClinicaEmTempoReal();
                }
            }
            
        } catch (NullPointerException e) {
            System.err.println("Null value encountered: " + e.getMessage());
            this.dynamicDateTime = "";
            this.pacientesIndisponiveisNomes = new ArrayList<>();
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid argument: " + e.getMessage());
            this.dynamicDateTime = "";
            this.pacientesIndisponiveisNomes = new ArrayList<>();
        }
    }

    public void onClinicaChange() {
        try {
            this.selectedSala = null;
            this.selectedData = null;
            this.selectedHorario = null;
            this.selectedPaciente = null;
            this.dynamicSelectedMedico = "";
            this.dynamicSelectedPaciente = "";
            this.dynamicSelectedSala = "";
            // NÃO limpa pacientesIndisponiveisNomes pois pacientes não dependem da clínica
            this.salasIndisponiveisNomes = new ArrayList<>();
            
            if (this.selectedClinicaId == null) {
                this.salasDisponiveis = new ArrayList<>();
                this.dynamicSelectedClinica = "";
                return;
            }

            Clinica clinica = clinicaService.consultar(this.selectedClinicaId);
            if (clinica == null) {
                this.salasDisponiveis = new ArrayList<>();
                this.dynamicSelectedClinica = "";
                return;
            }

            updateDynamicClinica();

            this.salasDisponiveis = clinica.getSalas().stream()
                .sorted(Comparator.comparing(Sala::getOrdem))
                .collect(Collectors.toList());

            if (selectedDate != null && selectedTime != null) {
                System.out.println("onClinicaChange - Verificando salas indisponíveis para data: " + selectedDate + " e hora: " + selectedTime);
                verificarSalasIndisponiveisEmTempoReal();
                verificarPacientesIndisponiveisEmTempoReal();
                verificarDisponibilidadeClinicaEmTempoReal();
            } else {
                System.out.println("onClinicaChange - Data ou hora não selecionados, não verificando salas indisponíveis");
            }

            PrimeFaces.current().ajax().update(":datetimeForm:clinica-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:sala-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:salas-indisponiveis-panel");
            PrimeFaces.current().ajax().update("scheduleForm:sala");

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao carregar dados da clínica: " + e.getMessage()));
            this.medicos = new ArrayList<>();
            this.salasDisponiveis = new ArrayList<>();
            this.dynamicSelectedClinica = "";
        }
    }

    public void onMedicoChange() {
        updateDynamicMedico();
        verificarDisponibilidadeMedicoEmTempoReal();
    }

            public void onPacienteChange() {
        updateDynamicPaciente();
        verificarDisponibilidadePacienteEmTempoReal();
    }  

     public void onSalaChange() {
        updateDynamicSala();
        verificarDisponibilidadeSalaEmTempoReal();
    }


public void verificarMedicosIndisponiveisEmTempoReal() {
        try {
            if (selectedDate == null || selectedTime == null) {
                this.medicosIndisponiveisNomes = new ArrayList<>();
                PrimeFaces.current().ajax().update(":datetimeForm:medicos-indisponiveis-panel");
                return;
            }

            Collection<Agenda> todosAgendamentos = agendaService.buscarTodosComRelacionamentos();
            LocalDateTime horarioSelecionado = LocalDateTime.of(selectedDate, selectedTime);
            List<String> medicosIndisponiveis = new ArrayList<>();
            Collection<Medico> todosMedicos = medicoService.buscarTodos();

            for (Medico medico : todosMedicos) {
                if (medico.getTempoConsulta() == null) continue;
                LocalDateTime horarioFim = horarioSelecionado.plusMinutes(medico.getTempoConsulta());

                Collection<Agenda> agendamentosDoDia = todosAgendamentos.stream()
                    .filter(a -> a.getMedico().getId().equals(medico.getId()) &&
                               Agenda.Status.AGENDADO.equals(a.getStatus()) &&
                               a.getDataHora() != null &&
                               a.getDataHoraFim() != null &&
                               a.getDataHora().toLocalDate().equals(horarioSelecionado.toLocalDate()))
                    .collect(Collectors.toList());

                boolean medicoIndisponivel = agendamentosDoDia.stream().anyMatch(agenda -> {
                    LocalDateTime agendaInicio = agenda.getDataHora();
                    LocalDateTime agendaFim = agenda.getDataHoraFim();
                    if (agendaInicio == null || agendaFim == null) return false;
                    return (horarioSelecionado.isEqual(agendaInicio) || 
                           horarioSelecionado.isAfter(agendaInicio)) && 
                           horarioSelecionado.isBefore(agendaFim) ||
                           (horarioFim.isAfter(agendaInicio) && 
                           (horarioFim.isEqual(agendaFim) || horarioFim.isBefore(agendaFim))) ||
                           (horarioSelecionado.isBefore(agendaInicio) && horarioFim.isAfter(agendaFim));
                });

                if (medicoIndisponivel) {
                    medicosIndisponiveis.add(medico.getNome());
                }
            }

            this.medicosIndisponiveisNomes = medicosIndisponiveis;
            PrimeFaces.current().ajax().update(":datetimeForm:medicos-indisponiveis-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:medico-panel"); // Atualiza também o autocomplete
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Erro ao verificar disponibilidade dos médicos: " + e.getMessage()));
            this.medicosIndisponiveisNomes = new ArrayList<>();
        }
    }   

 public void verificarPacientesIndisponiveisEmTempoReal() {
        try {
            System.out.println("verificarPacientesIndisponiveisEmTempoReal - Iniciando verificação");
            System.out.println("verificarPacientesIndisponiveisEmTempoReal - selectedDate: " + selectedDate + ", selectedTime: " + selectedTime);
            
            if (selectedDate == null || selectedTime == null) {
                System.out.println("verificarPacientesIndisponiveisEmTempoReal - Data ou hora não selecionados, limpando array");
                this.pacientesIndisponiveisNomes = new ArrayList<>();
                PrimeFaces.current().ajax().update(":datetimeForm:pacientes-indisponiveis-panel");
                return;
            }

            Collection<Agenda> todosAgendamentos = agendaService.buscarTodosComRelacionamentos();
            LocalDateTime horarioSelecionado = LocalDateTime.of(selectedDate, selectedTime);
            List<String> pacientesIndisponiveis = new ArrayList<>();
            Collection<Paciente> todosPacientes = pacienteService.buscarTodos();
            
            System.out.println("verificarPacientesIndisponiveisEmTempoReal - Total de pacientes: " + todosPacientes.size());

            for (Paciente paciente : todosPacientes) {
                Collection<Agenda> agendamentosDoDia = todosAgendamentos.stream()
                    .filter(a -> a.getPaciente().getId().equals(paciente.getId()) &&
                               Agenda.Status.AGENDADO.equals(a.getStatus()) &&
                               a.getDataHora() != null &&
                               a.getDataHoraFim() != null &&
                               a.getDataHora().toLocalDate().equals(horarioSelecionado.toLocalDate()))
                    .collect(Collectors.toList());

                boolean pacienteIndisponivel = agendamentosDoDia.stream().anyMatch(agenda -> {
                    LocalDateTime agendaInicio = agenda.getDataHora();
                    LocalDateTime agendaFim = agenda.getDataHoraFim();

                    return (horarioSelecionado.isEqual(agendaInicio) || 
                           horarioSelecionado.isAfter(agendaInicio)) && 
                           horarioSelecionado.isBefore(agendaFim) ||
                           (horarioSelecionado.plusHours(1).isAfter(agendaInicio) && 
                           (horarioSelecionado.plusHours(1).isEqual(agendaFim) || horarioSelecionado.plusHours(1).isBefore(agendaFim))) ||
                           (horarioSelecionado.isBefore(agendaInicio) && horarioSelecionado.plusHours(1).isAfter(agendaFim));
                });

                if (pacienteIndisponivel) {
                    pacientesIndisponiveis.add(paciente.getNome());
                    System.out.println("verificarPacientesIndisponiveisEmTempoReal - Paciente indisponível encontrado: " + paciente.getNome());
                }
            }

            this.pacientesIndisponiveisNomes = pacientesIndisponiveis;
            System.out.println("verificarPacientesIndisponiveisEmTempoReal - Total de pacientes indisponíveis: " + pacientesIndisponiveis.size());
            PrimeFaces.current().ajax().update(":datetimeForm:pacientes-indisponiveis-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:paciente-panel");
        } catch (Exception e) {
            System.err.println("verificarPacientesIndisponiveisEmTempoReal - Erro: " + e.getMessage());
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Erro ao verificar disponibilidade dos pacientes: " + e.getMessage()));
            this.pacientesIndisponiveisNomes = new ArrayList<>();
        }
    }

public void verificarSalasIndisponiveisEmTempoReal() {
        try {
            System.out.println("verificarSalasIndisponiveisEmTempoReal - Iniciando verificação");
            System.out.println("verificarSalasIndisponiveisEmTempoReal - selectedDate: " + selectedDate + ", selectedTime: " + selectedTime + ", selectedClinicaId: " + selectedClinicaId);
            
            if (selectedDate == null || selectedTime == null) {
                System.out.println("verificarSalasIndisponiveisEmTempoReal - Data ou hora não selecionados, limpando array");
                this.salasIndisponiveisNomes = new ArrayList<>();
                PrimeFaces.current().ajax().update(":datetimeForm:salas-indisponiveis-panel");
                return;
            }
            
            if (selectedClinicaId == null) {
                System.out.println("verificarSalasIndisponiveisEmTempoReal - Clínica não selecionada, limpando array");
                this.salasIndisponiveisNomes = new ArrayList<>();
                PrimeFaces.current().ajax().update(":datetimeForm:salas-indisponiveis-panel");
                return;
            }
            
            Collection<Agenda> todosAgendamentos = agendaService.buscarTodosComRelacionamentos();
            LocalDateTime horarioSelecionado = LocalDateTime.of(selectedDate, selectedTime);
            List<String> salasIndisponiveis = new ArrayList<>();
            
            Collection<Sala> todasSalas = salaService.buscarPorClinica(selectedClinicaId);
            System.out.println("verificarSalasIndisponiveisEmTempoReal - Total de salas da clínica: " + todasSalas.size());
            
            for (Sala sala : todasSalas) {
                Collection<Agenda> agendamentosDoDia = todosAgendamentos.stream()
                    .filter(a -> a.getNumeroSala().equals(sala.getOrdem()) &&
                               Agenda.Status.AGENDADO.equals(a.getStatus()) &&
                               a.getDataHora() != null &&
                               a.getDataHoraFim() != null &&
                               a.getDataHora().toLocalDate().equals(horarioSelecionado.toLocalDate()) &&
                               a.getClinica().getId().equals(selectedClinicaId)) // Filtra pela clínica selecionada
                    .collect(Collectors.toList());
                    
                boolean salaIndisponivel = agendamentosDoDia.stream().anyMatch(agenda -> {
                    LocalDateTime agendaInicio = agenda.getDataHora();
                    LocalDateTime agendaFim = agenda.getDataHoraFim();
                    return (horarioSelecionado.isEqual(agendaInicio) || 
                           horarioSelecionado.isAfter(agendaInicio)) && 
                           horarioSelecionado.isBefore(agendaFim) ||
                           (horarioSelecionado.plusHours(1).isAfter(agendaInicio) && 
                           (horarioSelecionado.plusHours(1).isEqual(agendaFim) || horarioSelecionado.plusHours(1).isBefore(agendaFim))) ||
                           (horarioSelecionado.isBefore(agendaInicio) && horarioSelecionado.plusHours(1).isAfter(agendaFim));
                });
                
                if (salaIndisponivel) {
                    salasIndisponiveis.add(sala.getNome());
                    System.out.println("verificarSalasIndisponiveisEmTempoReal - Sala indisponível encontrada: " + sala.getNome());
                }
            }
            
            this.salasIndisponiveisNomes = salasIndisponiveis;
            System.out.println("verificarSalasIndisponiveisEmTempoReal - Total de salas indisponíveis: " + salasIndisponiveis.size());
            PrimeFaces.current().ajax().update(":datetimeForm:salas-indisponiveis-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:sala-panel");
            PrimeFaces.current().ajax().update("scheduleForm:sala");
        } catch (Exception e) {
            System.err.println("verificarSalasIndisponiveisEmTempoReal - Erro: " + e.getMessage());
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Erro ao verificar disponibilidade das salas: " + e.getMessage()));
            this.salasIndisponiveisNomes = new ArrayList<>();
        }
    }

    public void verificarDisponibilidadeMedicoEmTempoReal() {
        if (selectedMedico != null && selectedDate != null && selectedTime != null) {
            try {
                LocalDateTime horarioSelecionado = LocalDateTime.of(selectedDate, selectedTime);
                
                LocalDateTime horarioFim = horarioSelecionado.plusMinutes(selectedMedico.getTempoConsulta());

                Collection<Agenda> agendamentosDoDia = agendaService.buscarTodosComRelacionamentos().stream()
                    .filter(a -> a.getMedico().getId().equals(selectedMedico.getId()) &&  
                               a.getStatus().equals(Agenda.Status.AGENDADO) &&
                               a.getDataHora() != null &&
                               a.getDataHora().toLocalDate().equals(horarioSelecionado.toLocalDate()))
                    .collect(Collectors.toList());

                boolean medicoIndisponivel = agendamentosDoDia.stream().anyMatch(agenda -> {
                    LocalDateTime agendaInicio = agenda.getDataHora();
                    LocalDateTime agendaFim = agenda.getDataHoraFim();

                    return (horarioSelecionado.isEqual(agendaInicio) || 
                           horarioSelecionado.isAfter(agendaInicio)) && 
                           horarioSelecionado.isBefore(agendaFim) ||
                           (horarioFim.isAfter(agendaInicio) && 
                           (horarioFim.isEqual(agendaFim) || horarioFim.isBefore(agendaFim))) ||
                           (horarioSelecionado.isBefore(agendaInicio) && horarioFim.isAfter(agendaFim));
                });

                if (medicoIndisponivel) {
                    FacesContext.getCurrentInstance().addMessage(null, 
                        new FacesMessage(FacesMessage.SEVERITY_WARN,
                            "Aviso", 
                            "O médico " + selectedMedico.getNome() + " não estará disponível no horário selecionado"));
                }
            } catch (Exception e) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                        "Erro", "Erro ao verificar disponibilidade do médico: " + e.getMessage()));
            }
        }
    }

public void verificarDisponibilidadeSalaEmTempoReal() {
        if (selectedSala != null && selectedDate != null && selectedTime != null && selectedClinicaId != null) {
            try {
                LocalDateTime horarioSelecionado = LocalDateTime.of(selectedDate, selectedTime);
                
                LocalDateTime horarioFim = horarioSelecionado.plusHours(1);

                Collection<Agenda> agendamentosDoDia = agendaService.buscarTodosComRelacionamentos().stream()
                    .filter(a -> a.getNumeroSala().equals(selectedSala.getOrdem()) &&  
                               a.getStatus().equals(Agenda.Status.AGENDADO) &&
                               a.getDataHora() != null &&
                               a.getDataHora().toLocalDate().equals(horarioSelecionado.toLocalDate()) &&
                               a.getClinica().getId().equals(selectedClinicaId)) 
                    .collect(Collectors.toList());

                boolean salaIndisponivel = agendamentosDoDia.stream().anyMatch(agenda -> {
                    LocalDateTime agendaInicio = agenda.getDataHora();
                    LocalDateTime agendaFim = agenda.getDataHoraFim();

                    return (horarioSelecionado.isEqual(agendaInicio) || 
                           horarioSelecionado.isAfter(agendaInicio)) && 
                           horarioSelecionado.isBefore(agendaFim) ||
                           (horarioFim.isAfter(agendaInicio) && 
                           (horarioFim.isEqual(agendaFim) || horarioFim.isBefore(agendaFim))) ||
                           (horarioSelecionado.isBefore(agendaInicio) && horarioFim.isAfter(agendaFim));
                });

                if (salaIndisponivel) {
                    FacesContext.getCurrentInstance().addMessage(null, 
                        new FacesMessage(FacesMessage.SEVERITY_WARN,
                            "Aviso", 
                            "A sala " + selectedSala.getNome() + " não estará disponível no horário selecionado"));
                }
            } catch (Exception e) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                        "Erro", "Erro ao verificar disponibilidade da sala: " + e.getMessage()));
            }
        }
    }

    public void verificarDisponibilidadePacienteEmTempoReal() {
        if (selectedPaciente != null && selectedDate != null && selectedTime != null) {
            try {
                LocalDateTime horarioSelecionado = LocalDateTime.of(selectedDate, selectedTime);
                
                LocalDateTime horarioFim = horarioSelecionado.plusHours(1);

                Collection<Agenda> agendamentosDoDia = agendaService.buscarTodosComRelacionamentos().stream()
                    .filter(a -> a.getPaciente().getId().equals(selectedPaciente.getId()) &&  
                               a.getStatus().equals(Agenda.Status.AGENDADO) &&
                               a.getDataHora() != null &&
                               a.getDataHora().toLocalDate().equals(horarioSelecionado.toLocalDate()))
                    .collect(Collectors.toList());

                boolean pacienteIndisponivel = agendamentosDoDia.stream().anyMatch(agenda -> {
                    LocalDateTime agendaInicio = agenda.getDataHora();
                    LocalDateTime agendaFim = agenda.getDataHoraFim();

                    return (horarioSelecionado.isEqual(agendaInicio) || 
                           horarioSelecionado.isAfter(agendaInicio)) && 
                           horarioSelecionado.isBefore(agendaFim) ||
                           (horarioFim.isAfter(agendaInicio) && 
                           (horarioFim.isEqual(agendaFim) || horarioFim.isBefore(agendaFim))) ||
                           (horarioSelecionado.isBefore(agendaInicio) && horarioFim.isAfter(agendaFim));
                });

                if (pacienteIndisponivel) {
                    FacesContext.getCurrentInstance().addMessage(null, 
                        new FacesMessage(FacesMessage.SEVERITY_WARN,
                            "Aviso", 
                            "O paciente " + selectedPaciente.getNome() + " não estará disponível no horário selecionado"));
                }
            } catch (Exception e) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                        "Erro", "Erro ao verificar disponibilidade do paciente: " + e.getMessage()));
            }
        }
    }

    public void verificarDisponibilidadeClinicaEmTempoReal() {
        if (selectedClinicaId != null && selectedDate != null && selectedTime != null) {
            try {
                LocalDateTime horarioSelecionado = LocalDateTime.of(selectedDate, selectedTime);
                
                boolean clinicaAberta = clinicaService.verificarHorarioFuncionamento(selectedClinicaId, horarioSelecionado);
                
                if (!clinicaAberta) {
                    String nomeClinica = "a clínica selecionada";
                    try {
                        Clinica clinica = clinicaService.consultar(selectedClinicaId);
                        if (clinica != null) {
                            nomeClinica = clinica.getNome();
                        }
                    } catch (Exception e) {
                        System.err.println("Erro ao buscar nome da clínica: " + e.getMessage());
                    }
                    
                    FacesContext.getCurrentInstance().addMessage(null, 
                        new FacesMessage(FacesMessage.SEVERITY_WARN,
                            "Aviso", 
                            "A clínica " + nomeClinica + " não estará aberta no horário selecionado"));
                }
            } catch (Exception e) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                        "Erro", "Erro ao verificar disponibilidade da clínica: " + e.getMessage()));
            }
        }
    }

    public String getItemStyleClass(String medicoNome) {
        if (medicosIndisponiveisNomes != null && medicosIndisponiveisNomes.contains(medicoNome)) {
            return "unavailable";
        }
        return "available";
    }
    
    public String getMedicoInputUnavailableClass() {
        if (selectedMedico != null && medicosIndisponiveisNomes != null && medicosIndisponiveisNomes.contains(selectedMedico.getNome())) {
            return "unavailable2";
        }
        return "available";
    }

    public String getPacienteItemStyleClass(String pacienteNome) {
        if (pacientesIndisponiveisNomes != null && pacientesIndisponiveisNomes.contains(pacienteNome)) {
            return "unavailable";
        }
        return "available";
    }
    
    public String getPacienteInputUnavailableClass() {
        if (selectedPaciente != null && pacientesIndisponiveisNomes != null && pacientesIndisponiveisNomes.contains(selectedPaciente.getNome())) {
            return "unavailable2";
        }
        return "available";
    }

        public String getSalaItemStyleClass(String salaNome) {
        System.out.println("getSalaItemStyleClass - Verificando sala: " + salaNome);
        System.out.println("getSalaItemStyleClass - salasIndisponiveisNomes: " + (salasIndisponiveisNomes != null ? salasIndisponiveisNomes.toString() : "null"));
        
        if (salasIndisponiveisNomes != null && salasIndisponiveisNomes.contains(salaNome)) {
            System.out.println("getSalaItemStyleClass - Sala indisponível: " + salaNome + " - Retornando 'unavailable'");
            return "unavailable";
        }
        System.out.println("getSalaItemStyleClass - Sala disponível: " + salaNome + " - Retornando 'available'");
        return "available";
    }
    
    public String getSalaInputUnavailableClass() {
        if (selectedSala != null && salasIndisponiveisNomes != null && salasIndisponiveisNomes.contains(selectedSala.getNome())) {
            return "unavailable2";
        }
        return "available";
    }

    public List<Medico> buscarMedicosDaClinica(String query) {
        if (selectedClinicaId == null) {
            return new ArrayList<>();
        }
        
        try {
            Collection<Medico> medicosClinica = medicoService.buscarPorClinica(selectedClinicaId);
            if (medicosClinica == null || medicosClinica.isEmpty()) {
                return new ArrayList<>();
            }
            
            String queryLowerCase = query.toLowerCase();
            List<Medico> medicosFiltrados = medicosClinica.stream()
                .filter(medico -> medico.getNome().toLowerCase().contains(queryLowerCase))
                .collect(Collectors.toList());

            PrimeFaces.current().ajax().update(":datetimeForm:medicos-indisponiveis-panel");
            
            return medicosFiltrados;
                
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao buscar médicos: " + e.getMessage()));
            return new ArrayList<>();
        }
    }

    public List<Sala> buscarSalasDaClinica(String query) {
        if (selectedClinicaId == null) {
            System.out.println("Clínica não selecionada");
            return new ArrayList<>();
        }
        
        try {
            System.out.println("Buscando salas para clínica ID: " + selectedClinicaId);
            Collection<Sala> salasClinica = salaService.buscarPorClinica(selectedClinicaId);
            
            if (salasClinica == null || salasClinica.isEmpty()) {
                System.out.println("Nenhuma sala encontrada para a clínica");
                return new ArrayList<>();
            }
            
            System.out.println("Total de salas encontradas: " + salasClinica.size());
            
            String queryLowerCase = query.toLowerCase();
            List<Sala> salasFiltradas = salasClinica.stream()
                .filter(sala -> {
                    String nomeSala = sala.getNome() != null ? sala.getNome().toLowerCase() : "";
                    return nomeSala.contains(queryLowerCase);
                })
                .sorted(Comparator.comparing(Sala::getOrdem))
                .collect(Collectors.toList());

            System.out.println("Salas filtradas: " + salasFiltradas.size());

            PrimeFaces.current().ajax().update(":datetimeForm:sala-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:salas-indisponiveis-panel");
            
            return salasFiltradas;
                
        } catch (Exception e) {
            System.err.println("Erro ao buscar salas: " + e.getMessage());
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao buscar salas: " + e.getMessage()));
            return new ArrayList<>();
        }
    }


     public void prepararEdicao(Agenda consulta) {
        try {
            this.editMedico = null;
            this.editSala = null;
            this.editObservacoes = null;
            this.editDataHora = null;
            this.dynamicEditSala = null;
            this.dynamicEditDateTime = null;
            
            this.selectedConsulta = consulta;
            if (selectedConsulta == null) {
                throw new IllegalStateException("Consulta não encontrada");
            }

            this.selectedClinicaId = selectedConsulta.getClinica().getId();
            this.medicoAtual = selectedConsulta.getMedico();
            
            Sala salaConsultaAtual = selectedConsulta.getSala();
            if (salaConsultaAtual != null) {
                this.salaAtual = salaConsultaAtual;
            } else {
                Integer salaOrdem = selectedConsulta.getNumeroSala();
                if (salaOrdem != null) {
                    try {
                        Collection<Sala> salasClinica = salaService.buscarPorClinica(selectedConsulta.getClinica().getId());
                        this.salaAtual = salasClinica.stream()
                            .filter(s -> salaOrdem.equals(s.getOrdem()))
                            .findFirst()
                            .orElse(null);
                    } catch (Exception ex) {
                        System.err.println("Erro ao buscar sala atual pela ordem: " + ex.getMessage());
                        this.salaAtual = null;
                    }
                }
            }
            
            System.out.println("Preparando edição - Sala atual definida: " + 
                             (salaAtual != null ? salaAtual.getNome() + " (ID: " + salaAtual.getId() + ", Ordem: " + salaAtual.getOrdem() + ")" : "null"));
            
            this.editDataHora = selectedConsulta.getDataHora();  
            this.editMedico = selectedConsulta.getMedico();
            
            Sala salaConsulta = selectedConsulta.getSala();
            if (salaConsulta != null) {
                this.editSala = salaConsulta;
            } else {
                Integer salaOrdem = selectedConsulta.getNumeroSala();
                if (salaOrdem != null) {
                    try {
                        Collection<Sala> salasClinica = salaService.buscarPorClinica(selectedConsulta.getClinica().getId());
                        this.editSala = salasClinica.stream()
                            .filter(s -> salaOrdem.equals(s.getOrdem()))
                            .findFirst()
                            .orElse(null);
                    } catch (Exception ex) {
                        System.err.println("Erro ao buscar sala pela ordem: " + ex.getMessage());
                        this.editSala = null;
                    }
                }
            }
            
            this.editObservacoes = selectedConsulta.getObservacoes();
            this.isUpdate = true;
            
            System.out.println("Preparando edição - Sala selecionada: " + 
                             (editSala != null ? editSala.getNome() + " (ID: " + editSala.getId() + ", Ordem: " + editSala.getOrdem() + ")" : "null") +
                             " - Sala da consulta: " + (salaConsulta != null ? salaConsulta.getNome() : "null") +
                             ", Ordem: " + selectedConsulta.getNumeroSala());
            
            updateDynamicEditDateTime();
            updateDynamicEditSala();

            if (FacesContext.getCurrentInstance() != null) {
                PrimeFaces.current().ajax().update(":editForm");
                
                verificarDisponibilidadeParaEdicaoEmTempoReal();
            }
            
            System.out.println("Edição preparada - Médico: " + (editMedico != null ? editMedico.getNome() : "null") + 
                             ", Sala: " + (editSala != null ? editSala.getNome() : "null"));
        } catch (Exception e) {
            this.selectedConsulta = null;
            this.editMedico = null;
            this.editSala = null;
            this.editObservacoes = null;
            this.editDataHora = null;
            this.dynamicEditSala = null;
            this.dynamicEditDateTime = null;
            this.medicoAtual = null;
            this.salaAtual = null;
            this.isUpdate = false;
            
            System.err.println("Erro ao preparar edição: " + e.getMessage());
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao preparar edição: " + e.getMessage()));
        }
    }

    public void salvarEdicao() {
        FacesContext context = FacesContext.getCurrentInstance();
        
        try {
            if (selectedConsulta == null) {
                context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Nenhuma consulta selecionada para edição"));
                context.validationFailed();
                return;
            }

            if (editMedico == null || editSala == null || editDataHora == null) {
                context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Todos os campos são obrigatórios"));
                context.validationFailed();
                return;
            }
            if (!verificarDisponibilidadeCompletaParaEdicao()) {
                context.validationFailed();
                return;
            }

            selectedConsulta.setMedico(editMedico);
            selectedConsulta.setSala(editSala);
            selectedConsulta.setNumeroSala(editSala.getOrdem());
            selectedConsulta.setDataHora(editDataHora);
            selectedConsulta.setDataHoraFim(editDataHora.plusMinutes(editMedico.getTempoConsulta()));
            selectedConsulta.setObservacoes(editObservacoes);

            agendaService.alterar(selectedConsulta);

            refreshAgendamentos();

            this.selectedConsulta = null;
            this.editMedico = null;
            this.editSala = null;
            this.editDataHora = null;
            this.editObservacoes = null;

            PrimeFaces.current().ajax().update(":datetimeForm:datetime-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:medico-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:clinica-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:paciente-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:medicos-indisponiveis-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:pacientes-indisponiveis-panel");
            PrimeFaces.current().ajax().update("scheduleForm:medico");
            PrimeFaces.current().ajax().update("scheduleForm:paciente");

            context.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Agendamento atualizado com sucesso"));

            // Limpa os arrays de indisponíveis após edição para atualizar o estado visual
            verificarMedicosIndisponiveisEmTempoReal();
            verificarPacientesIndisponiveisEmTempoReal();
            verificarSalasIndisponiveisEmTempoReal();

        } catch (Exception e) {
            context.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao atualizar agendamento: " + e.getMessage()));
            context.validationFailed();
        }
    }

    public void cancelarEdicao() {
        this.selectedConsulta = null;
        this.selectedMedico = null;
        this.selectedPaciente = null;
        this.selectedClinicaId = null;
        this.selectedSala = null;
        this.selectedData = null;
        this.selectedTime = null;
        this.observacoes = null;
        this.isUpdate = false;
        
        this.dynamicDateTime = "";
        this.dynamicSelectedMedico = "";
        this.dynamicSelectedClinica = "";
        this.dynamicSelectedPaciente = "";
        this.dynamicSelectedSala = "";
        this.medicosIndisponiveisNomes = new ArrayList<>();
        this.pacientesIndisponiveisNomes = new ArrayList<>();
        this.salasIndisponiveisNomes = new ArrayList<>();
        
        if (FacesContext.getCurrentInstance() != null) {
            PrimeFaces.current().ajax().update(":datetimeForm:datetime-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:medico-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:clinica-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:paciente-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:medicos-indisponiveis-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:pacientes-indisponiveis-panel");
        }
    }

   private boolean verificarDisponibilidadeParaEdicao() throws BusinessException {
        return verificarDisponibilidadeParaEdicaoCompletaV2();
    }

    private boolean verificarDisponibilidadeParaEdicaoCompletaV2() throws BusinessException {
        if (editMedico == null || editSala == null || editDataHora == null || selectedConsulta == null) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", 
                    "Todos os campos são necessários para verificar a disponibilidade"));
            return false;
        }

        LocalDateTime editHorarioFim = editDataHora.plusMinutes(editMedico.getTempoConsulta());

        try {
            Collection<Agenda> agendamentosDoDia = agendaService.buscarTodosComRelacionamentos().stream()
                .filter(a -> Agenda.Status.AGENDADO.equals(a.getStatus()) &&
                            !a.getId().equals(selectedConsulta.getId()) // Exclui a própria consulta
                            && a.getDataHora() != null
                            && a.getDataHora().toLocalDate().equals(editDataHora.toLocalDate()))
                .collect(Collectors.toList());

            boolean medicoIndisponivel = agendamentosDoDia.stream()
                .filter(a -> a.getMedico().getId().equals(editMedico.getId()))
                .anyMatch(agenda -> temSobreposicaoDeHorario(editDataHora, editHorarioFim, agenda.getDataHora(), agenda.getDataHoraFim()));

            if (medicoIndisponivel) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", 
                        "O médico " + editMedico.getNome() + " não está disponível no horário selecionado"));
                return false;
            }

            boolean salaIndisponivel = agendamentosDoDia.stream()
                .filter(a -> a.getNumeroSala().equals(editSala.getOrdem()))
                .anyMatch(agenda -> temSobreposicaoDeHorario(editDataHora, editHorarioFim, agenda.getDataHora(), agenda.getDataHoraFim()));

            if (salaIndisponivel) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", 
                        "A sala " + editSala.getNome() + " não está disponível no horário selecionado"));
                return false;
            }

            return true;

        } catch (BusinessException e) {
            throw new BusinessException("Erro ao buscar agendamentos: " + e.getMessage(), e);
        }
    }


    private boolean temSobreposicaoDeHorario(LocalDateTime inicio1, LocalDateTime fim1, LocalDateTime inicio2, LocalDateTime fim2) {
        return (inicio1.isEqual(inicio2) || inicio1.isAfter(inicio2)) && inicio1.isBefore(fim2) ||
               (fim1.isAfter(inicio2) && (fim1.isEqual(fim2) || fim1.isBefore(fim2))) ||
               (inicio1.isBefore(inicio2) && fim1.isAfter(fim2));
    }

    
    public List<Medico> getMedicosDisponiveisParaEdicao(String query) {
        if (selectedClinicaId == null || editDataHora == null) {
            return new ArrayList<>();
        }
        
        try {
            Collection<Medico> medicosClinica = medicoService.buscarPorClinica(selectedClinicaId);
            if (medicosClinica == null || medicosClinica.isEmpty()) {
                return new ArrayList<>();
            }
            
            String queryLowerCase = query.toLowerCase();
            List<Medico> medicosFiltrados = medicosClinica.stream()
                .filter(medico -> medico.getNome().toLowerCase().contains(queryLowerCase) && 
                       (medicoAtual == null || !medico.getId().equals(medicoAtual.getId())))
                .collect(Collectors.toList());

            LocalDateTime horarioFim = editDataHora.plusMinutes(editMedico != null ? editMedico.getTempoConsulta() : 30);
            Collection<Agenda> todosAgendamentos = agendaService.buscarTodosComRelacionamentos();
            
            medicosFiltrados = medicosFiltrados.stream()
                .filter(medico -> {
                    Collection<Agenda> agendamentosDoDia = todosAgendamentos.stream()
                        .filter(a -> a.getMedico().getId().equals(medico.getId()) &&
                                   Agenda.Status.AGENDADO.equals(a.getStatus()) &&
                                   a.getDataHora() != null &&
                                   a.getDataHoraFim() != null &&
                                   a.getDataHora().toLocalDate().equals(editDataHora.toLocalDate()) &&
                                   !a.getId().equals(selectedConsulta.getId()))
                        .collect(Collectors.toList());

                    return !agendamentosDoDia.stream().anyMatch(agenda -> {
                        LocalDateTime agendaInicio = agenda.getDataHora();
                        LocalDateTime agendaFim = agenda.getDataHoraFim();
                        if (agendaInicio == null || agendaFim == null) return false;
                        
                        return (editDataHora.isEqual(agendaInicio) || 
                               editDataHora.isAfter(agendaInicio)) && 
                               editDataHora.isBefore(agendaFim) ||
                               (horarioFim.isAfter(agendaInicio) && 
                               (horarioFim.isEqual(agendaFim) || horarioFim.isBefore(agendaFim))) ||
                               (editDataHora.isBefore(agendaInicio) && horarioFim.isAfter(agendaFim));
                    });
                })
                .collect(Collectors.toList());

            if (medicoAtual != null) {
                Collection<Agenda> agendamentosDoDia = todosAgendamentos.stream()
                    .filter(a -> a.getMedico().getId().equals(medicoAtual.getId()) &&
                               Agenda.Status.AGENDADO.equals(a.getStatus()) &&
                               a.getDataHora() != null &&
                               a.getDataHoraFim() != null &&
                               a.getDataHora().toLocalDate().equals(editDataHora.toLocalDate()) &&
                               !a.getId().equals(selectedConsulta.getId()))
                    .collect(Collectors.toList());

                boolean disponivel = !agendamentosDoDia.stream().anyMatch(agenda -> {
                    LocalDateTime agendaInicio = agenda.getDataHora();
                    LocalDateTime agendaFim = agenda.getDataHoraFim();
                    if (agendaInicio == null || agendaFim == null) return false;
                    
                    return (editDataHora.isEqual(agendaInicio) || 
                           editDataHora.isAfter(agendaInicio)) && 
                           editDataHora.isBefore(agendaFim) ||
                           (horarioFim.isAfter(agendaInicio) && 
                           (horarioFim.isEqual(agendaFim) || horarioFim.isBefore(agendaFim))) ||
                           (editDataHora.isBefore(agendaInicio) && horarioFim.isAfter(agendaFim));
                });

                if (disponivel) {
                    medicosFiltrados.add(medicoAtual);
                }
            }

            return medicosFiltrados;
                
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao buscar médicos: " + e.getMessage()));
            return new ArrayList<>();
        }
    }

    public List<Sala> buscarSalasDisponiveisParaEdicao(String query) {
        if (selectedClinicaId == null || editDataHora == null) {
            return new ArrayList<>();
        }
        
        try {
            Collection<Sala> salasClinica = salaService.buscarPorClinica(selectedClinicaId);
            
            System.out.println("BuscarSalasDisponiveisParaEdicao - Total de salas na clínica: " + 
                             (salasClinica != null ? salasClinica.size() : 0));
            
            if (salasClinica == null || salasClinica.isEmpty()) {
                System.out.println("BuscarSalasDisponiveisParaEdicao - Nenhuma sala encontrada");
                return new ArrayList<>();
            }
            
            String queryLowerCase = query.toLowerCase();
            List<Sala> salasFiltradas = salasClinica.stream()
                .filter(sala -> sala.getNome().toLowerCase().contains(queryLowerCase) && 
                       (salaAtual == null || !sala.getOrdem().equals(salaAtual.getOrdem())))
                .collect(Collectors.toList());

            LocalDateTime horarioFim = editDataHora.plusMinutes(editMedico != null ? editMedico.getTempoConsulta() : 30);
            Collection<Agenda> todosAgendamentos = agendaService.buscarTodosComRelacionamentos();
            
            salasFiltradas = salasFiltradas.stream()
                .filter(sala -> {
                    Collection<Agenda> agendamentosDoDia = todosAgendamentos.stream()
                        .filter(a -> a.getNumeroSala().equals(sala.getOrdem()) &&
                                   Agenda.Status.AGENDADO.equals(a.getStatus()) &&
                                   a.getDataHora() != null &&
                                   a.getDataHoraFim() != null &&
                                   a.getDataHora().toLocalDate().equals(editDataHora.toLocalDate()) &&
                                   !a.getId().equals(selectedConsulta.getId())) // Exclui a consulta atual
                        .collect(Collectors.toList());
                    
                    System.out.println("BuscarSalasDisponiveisParaEdicao - Sala " + sala.getNome() + 
                                     " tem " + agendamentosDoDia.size() + " agendamentos no dia");

                    // Verifica se há sobreposição de horários
                    boolean disponivel = !agendamentosDoDia.stream().anyMatch(agenda -> {
                        LocalDateTime agendaInicio = agenda.getDataHora();
                        LocalDateTime agendaFim = agenda.getDataHoraFim();
                        if (agendaInicio == null || agendaFim == null) return false;
                        
                        boolean temSobreposicao = (editDataHora.isEqual(agendaInicio) || 
                                                 editDataHora.isAfter(agendaInicio)) && 
                                                 editDataHora.isBefore(agendaFim) ||
                                                 (horarioFim.isAfter(agendaInicio) && 
                                                 (horarioFim.isEqual(agendaFim) || horarioFim.isBefore(agendaFim))) ||
                                                 (editDataHora.isBefore(agendaInicio) && horarioFim.isAfter(agendaFim));
                        
                        System.out.println("BuscarSalasDisponiveisParaEdicao - Sala " + sala.getNome() + 
                                         " tem sobreposição com agendamento " + agenda.getId() + "? " + temSobreposicao);
                        
                        return temSobreposicao;
                    });
                    
                    System.out.println("BuscarSalasDisponiveisParaEdicao - Sala " + sala.getNome() + 
                                     " está disponível? " + disponivel);
                    
                    return disponivel;
                })
                .collect(Collectors.toList());

            // Verifica se a sala atual está disponível e adiciona se estiver
            if (salaAtual != null) {
                Collection<Agenda> agendamentosDoDia = todosAgendamentos.stream()
                    .filter(a -> a.getNumeroSala().equals(salaAtual.getOrdem()) &&
                               Agenda.Status.AGENDADO.equals(a.getStatus()) &&
                               a.getDataHora() != null &&
                               a.getDataHoraFim() != null &&
                               a.getDataHora().toLocalDate().equals(editDataHora.toLocalDate()) &&
                               !a.getId().equals(selectedConsulta.getId())) // Exclui a consulta atual
                    .collect(Collectors.toList());

                boolean disponivel = !agendamentosDoDia.stream().anyMatch(agenda -> {
                    LocalDateTime agendaInicio = agenda.getDataHora();
                    LocalDateTime agendaFim = agenda.getDataHoraFim();
                    if (agendaInicio == null || agendaFim == null) return false;
                    
                    boolean temSobreposicao = (editDataHora.isEqual(agendaInicio) || 
                                             editDataHora.isAfter(agendaInicio)) && 
                                             editDataHora.isBefore(agendaFim) ||
                                             (horarioFim.isAfter(agendaInicio) && 
                                             (horarioFim.isEqual(agendaFim) || horarioFim.isBefore(agendaFim))) ||
                                             (editDataHora.isBefore(agendaInicio) && horarioFim.isAfter(agendaFim));
                    
                    System.out.println("BuscarSalasDisponiveisParaEdicao - Sala atual " + salaAtual.getNome() + 
                                     " tem sobreposição com agendamento " + agenda.getId() + "? " + temSobreposicao);
                    
                    return temSobreposicao;
                });

                if (disponivel) {
                    System.out.println("BuscarSalasDisponiveisParaEdicao - Sala atual " + salaAtual.getNome() + 
                                     " está disponível, adicionando à lista");
                    salasFiltradas.add(salaAtual);
                } else {
                    System.out.println("BuscarSalasDisponiveisParaEdicao - Sala atual " + salaAtual.getNome() + 
                                     " NÃO está disponível, NÃO adicionando à lista");
                }
            }

            System.out.println("BuscarSalasDisponiveisParaEdicao - Total de salas disponíveis: " + 
                             salasFiltradas.size());

            return salasFiltradas;
                
        } catch (Exception e) {
            System.err.println("BuscarSalasDisponiveisParaEdicao - Erro ao buscar salas: " + e.getMessage());
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao buscar salas: " + e.getMessage()));
            return new ArrayList<>();
        }
    }

    // Atualiza a string dinâmica de data/hora para edição
    public void updateDynamicEditDateTime() {
        if (editDataHora != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            dynamicEditDateTime = editDataHora.format(formatter);
            
            List<Medico> medicosDisponiveis = getMedicosDisponiveisParaEdicao("");
            List<Sala> salasDisponiveis = buscarSalasDisponiveisParaEdicao("");
            
            boolean medicoAtualDisponivel = editMedico != null && 
                medicosDisponiveis.stream().anyMatch(m -> m.getId().equals(editMedico.getId()));
            
            boolean salaAtualDisponivel = editSala != null && 
                salasDisponiveis.stream().anyMatch(s -> s.getId().equals(editSala.getId()));
            
            if (editMedico != null && !medicoAtualDisponivel) {
                System.out.println("Médico atual não disponível no novo horário, limpando campo");
                editMedico = null;
            }
            
            if (editSala != null && !salaAtualDisponivel) {
                System.out.println("Sala atual não disponível no novo horário, limpando campo");
                editSala = null;
            }

            if (medicoAtual != null) {
                FacesContext.getCurrentInstance().getExternalContext().getSessionMap()
                    .put("medicoAtual", medicoAtual);
            }
            if (salaAtual != null) {
                FacesContext.getCurrentInstance().getExternalContext().getSessionMap()
                    .put("salaAtual", salaAtual);
            }

            updateDynamicEditSala();
        } else {
            dynamicEditDateTime = null;
        }
    }

    public boolean isMedicoEditDisabled() {
        if (editDataHora == null || selectedClinicaId == null) {
            return true;
        }
        
        List<Medico> medicosDisponiveis = getMedicosDisponiveisParaEdicao("");
        return medicosDisponiveis.isEmpty();
    }
    
    public boolean isSalaEditDisabled() {
        if (editDataHora == null || selectedClinicaId == null) {
            return true;
        }
        
        List<Sala> salasDisponiveis = buscarSalasDisponiveisParaEdicao("");
        return salasDisponiveis.isEmpty();
    }
    
    public String getMedicoEditInputClass() {
        if (isMedicoEditDisabled()) {
            return "ui-state-disabled";
        }
        return "";
    }
    

    public String getSalaEditInputClass() {
        if (isSalaEditDisabled()) {
            return "ui-state-disabled";
        }
        return "";
    }
    
    public String getMedicoEditPlaceholder() {
        if (isMedicoEditDisabled()) {
            return "Nenhum médico disponível";
        }
        return "Digite o nome do médico";
    }
    
    public String getSalaEditPlaceholder() {
        if (isSalaEditDisabled()) {
            return "Nenhuma sala disponível";
        }
        return "Digite o nome da sala";
    }

    public void verificarDisponibilidadeParaEdicaoEmTempoReal() {
        PrimeFaces.current().ajax().update(":editForm");
    }

        public List<Sala> getSalasDisponiveisParaEdicao() {
        return buscarSalasDisponiveisParaEdicao("");
    }

    public List<Medico> buscarMedicosDisponiveisParaEdicao(String query) {
        if (selectedClinicaId == null || editDataHora == null || editSala == null) {
            return new ArrayList<>();
        }
        
        try {
            Collection<Medico> medicosClinica = medicoService.buscarPorClinica(selectedClinicaId);
            if (medicosClinica == null || medicosClinica.isEmpty()) {
                return new ArrayList<>();
            }
            
            String queryLowerCase = query.toLowerCase();
            // Remove o médico atual da lista inicial para evitar duplicação
            List<Medico> medicosFiltrados = medicosClinica.stream()
                .filter(medico -> medico.getNome().toLowerCase().contains(queryLowerCase) && 
                       (medicoAtual == null || !medico.getId().equals(medicoAtual.getId())))
                .collect(Collectors.toList());

            LocalDateTime horarioFim = editDataHora.plusMinutes(editMedico.getTempoConsulta());
            Collection<Agenda> todosAgendamentos = agendaService.buscarTodosComRelacionamentos();
            
            medicosFiltrados = medicosFiltrados.stream()
                .filter(medico -> {
                    Collection<Agenda> agendamentosDoDia = todosAgendamentos.stream()
                        .filter(a -> a.getMedico().getId().equals(medico.getId()) &&
                                   Agenda.Status.AGENDADO.equals(a.getStatus()) &&
                                   a.getDataHora() != null &&
                                   a.getDataHoraFim() != null &&
                                   a.getDataHora().toLocalDate().equals(editDataHora.toLocalDate()) &&
                                   !a.getId().equals(selectedConsulta.getId()))
                        .collect(Collectors.toList());

                    return !agendamentosDoDia.stream().anyMatch(agenda -> {
                        LocalDateTime agendaInicio = agenda.getDataHora();
                        LocalDateTime agendaFim = agenda.getDataHoraFim();
                        if (agendaInicio == null || agendaFim == null) return false;
                        
                        return (editDataHora.isEqual(agendaInicio) || 
                               editDataHora.isAfter(agendaInicio)) && 
                               editDataHora.isBefore(agendaFim) ||
                               (horarioFim.isAfter(agendaInicio) && 
                               (horarioFim.isEqual(agendaFim) || horarioFim.isBefore(agendaFim))) ||
                               (editDataHora.isBefore(agendaInicio) && horarioFim.isAfter(agendaFim));
                    });
                })
                .collect(Collectors.toList());

            if (medicoAtual != null) {
                Collection<Agenda> agendamentosDoDia = todosAgendamentos.stream()
                    .filter(a -> a.getMedico().getId().equals(medicoAtual.getId()) &&
                               Agenda.Status.AGENDADO.equals(a.getStatus()) &&
                               a.getDataHora() != null &&
                               a.getDataHoraFim() != null &&
                               a.getDataHora().toLocalDate().equals(editDataHora.toLocalDate()) &&
                               !a.getId().equals(selectedConsulta.getId()))
                    .collect(Collectors.toList());

                boolean disponivel = !agendamentosDoDia.stream().anyMatch(agenda -> {
                    LocalDateTime agendaInicio = agenda.getDataHora();
                    LocalDateTime agendaFim = agenda.getDataHoraFim();
                    if (agendaInicio == null || agendaFim == null) return false;
                    
                    return (editDataHora.isEqual(agendaInicio) || 
                           editDataHora.isAfter(agendaInicio)) && 
                           editDataHora.isBefore(agendaFim) ||
                           (horarioFim.isAfter(agendaInicio) && 
                           (horarioFim.isEqual(agendaFim) || horarioFim.isBefore(agendaFim))) ||
                           (editDataHora.isBefore(agendaInicio) && horarioFim.isAfter(agendaFim));
                });

                if (disponivel) {
                    medicosFiltrados.add(medicoAtual);
                }
            }

            return medicosFiltrados;
                
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao buscar médicos: " + e.getMessage()));
            return new ArrayList<>();
        }
    }

    private boolean verificarDisponibilidadeCompletaParaEdicao() {
        FacesContext context = FacesContext.getCurrentInstance();
        
        try {
            if (editMedico == null || editSala == null || editDataHora == null || selectedConsulta == null) {
                context.addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", 
                        "Todos os campos são necessários para verificar a disponibilidade"));
                return false;
            }

            LocalDateTime editHorarioFim = editDataHora.plusMinutes(editMedico.getTempoConsulta());
            Paciente pacienteAtual = selectedConsulta.getPaciente();

            Collection<Agenda> agendamentosDoDia = agendaService.buscarTodosComRelacionamentos().stream()
                .filter(a -> Agenda.Status.AGENDADO.equals(a.getStatus()) &&
                            !a.getId().equals(selectedConsulta.getId()) // Exclui a própria consulta
                            && a.getDataHora() != null
                            && a.getDataHora().toLocalDate().equals(editDataHora.toLocalDate()))
                .collect(Collectors.toList());

            boolean medicoIndisponivel = agendamentosDoDia.stream()
                .filter(a -> a.getMedico().getId().equals(editMedico.getId()))
                .anyMatch(agenda -> temSobreposicaoDeHorario(editDataHora, editHorarioFim, agenda.getDataHora(), agenda.getDataHoraFim()));

            if (medicoIndisponivel) {
                context.addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", 
                        "O médico " + editMedico.getNome() + " não está disponível no horário selecionado"));
                return false;
            }

            boolean salaIndisponivel = agendamentosDoDia.stream()
                .filter(a -> a.getNumeroSala().equals(editSala.getOrdem()))
                .anyMatch(agenda -> temSobreposicaoDeHorario(editDataHora, editHorarioFim, agenda.getDataHora(), agenda.getDataHoraFim()));

            if (salaIndisponivel) {
                context.addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", 
                        "A sala " + editSala.getNome() + " não está disponível no horário selecionado"));
                return false;
            }

            boolean pacienteIndisponivel = agendamentosDoDia.stream()
                .filter(a -> a.getPaciente().getId().equals(pacienteAtual.getId()))
                .anyMatch(agenda -> temSobreposicaoDeHorario(editDataHora, editHorarioFim, agenda.getDataHora(), agenda.getDataHoraFim()));

            if (pacienteIndisponivel) {
                context.addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", 
                        "O paciente " + pacienteAtual.getNome() + " não está disponível no horário selecionado"));
                return false;
            }

            return true;

        } catch (Exception e) {
            context.addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", 
                    "Erro ao verificar disponibilidade: " + e.getMessage()));
            return false;
        }
    }



    public void updateDynamicEditSala() {
        if (editSala != null) {
            this.dynamicEditSala = editSala.getNome();
            System.out.println("UpdateDynamicEditSala - Sala editada: " + editSala.getNome() + 
                             " (ID: " + editSala.getId() + ", Ordem: " + editSala.getOrdem() + ")");
        } else {
            this.dynamicEditSala = "";
            System.out.println("UpdateDynamicEditSala - Nenhuma sala selecionada");
        }
        
        if (FacesContext.getCurrentInstance() != null) {
            PrimeFaces.current().ajax().update(":editForm:editSalaId");
            System.out.println("UpdateDynamicEditSala - UI atualizada com valor: " + this.dynamicEditSala);
        }
    }

    public void onSalaEditChange() {
        updateDynamicEditSala();
    }

    public String getDynamicEditSala() {
        return dynamicEditSala;
    }

     private String gerarHashAgendamentos() {
        try {
            Collection<Agenda> agendamentos = agendaService.buscarTodosComRelacionamentos();
            if (agendamentos == null || agendamentos.isEmpty()) {
                return "empty";
            }
            
            StringBuilder hashBuilder = new StringBuilder();
            agendamentos.stream()
                .sorted((a1, a2) -> a1.getId().compareTo(a2.getId()))
                .forEach(agenda -> {
                    hashBuilder.append(agenda.getId())
                              .append("|")
                              .append(agenda.getStatus())
                              .append("|")
                              .append(agenda.getDataHora())
                              .append("|")
                              .append(agenda.getDataHoraFim())
                              .append("|");
                });
            
            return String.valueOf(hashBuilder.toString().hashCode());
            
        } catch (Exception e) {
            System.err.println("Erro ao gerar hash dos agendamentos: " + e.getMessage());
            return "error";
        }
    }
    

    public boolean verificarMudancasAgendamentos() {
        try {
            String hashAtual = gerarHashAgendamentos();
            
            if (!hashAtual.equals(hashAgendamentosAtual)) {
                System.out.println("Mudança detectada nos agendamentos - Hash anterior: " + 
                                 hashAgendamentosAtual + ", Hash atual: " + hashAtual);
                
                this.hashAgendamentosAtual = hashAtual;
                this.mudancaDetectada = true;
                
                refreshAgendamentos();
                
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            System.err.println("Erro ao verificar mudanças nos agendamentos: " + e.getMessage());
            return false;
        }
    }
    
    public void verificarMudancasEAtualizar() {
        try {
            boolean houveMudanca = verificarMudancasAgendamentos();
            
            if (houveMudanca) {
                System.out.println("Atualizando interface devido a mudanças nos agendamentos");
                
                refreshAgendamentos();
                
                if (FacesContext.getCurrentInstance() != null) {
                    PrimeFaces.current().ajax().update(":agendaTable");
                    PrimeFaces.current().ajax().update(":datetimeForm:datetime-panel");
                    PrimeFaces.current().ajax().update(":datetimeForm:medico-panel");
                    PrimeFaces.current().ajax().update(":datetimeForm:clinica-panel");
                    PrimeFaces.current().ajax().update(":datetimeForm:paciente-panel");
                    PrimeFaces.current().ajax().update(":datetimeForm:medicos-indisponiveis-panel");
                    PrimeFaces.current().ajax().update(":datetimeForm:pacientes-indisponiveis-panel");
                    PrimeFaces.current().ajax().update(":datetimeForm:sala-panel");
                    PrimeFaces.current().ajax().update(":datetimeForm:salas-indisponiveis-panel");
                }
                
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, 
                        "Atualização Automática", "Lista de agendamentos atualizada automaticamente"));
            }
            
        } catch (Exception e) {
            System.err.println("Erro ao verificar mudanças e atualizar: " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Erro ao verificar mudanças: " + e.getMessage()));
        }
    }
    
    public boolean isMudancaDetectada() {
        return mudancaDetectada;
    }
    
    public void resetarMudancaDetectada() {
        this.mudancaDetectada = false;
    }
    
    /*
    Executa manualmente a verificação automática de agendamentos para conclusão
    Útil para testes e para forçar uma verificação imediata
     */
    public void executarVerificacaoAutomaticaManual() {
        FacesContext context = FacesContext.getCurrentInstance();
        
        try {
            Collection<Agenda> agendamentosParaConcluir = agendaService.buscarAgendamentosParaConclusaoAutomatica();
            
            if (agendamentosParaConcluir.isEmpty()) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, 
                    "Informação", "Nenhum agendamento encontrado para conclusão automática"));
            } else {
                agendaService.concluirAgendamentosAutomaticamente();
                
                refreshAgendamentos();
                
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, 
                    "Sucesso", agendamentosParaConcluir.size() + " agendamento(s) concluído(s) automaticamente"));
            }
            
            PrimeFaces.current().ajax().update(":datetimeForm:datetime-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:medico-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:clinica-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:paciente-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:medicos-indisponiveis-panel");
            PrimeFaces.current().ajax().update(":datetimeForm:pacientes-indisponiveis-panel");
            
        } catch (BusinessException e) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                "Erro", "Erro ao executar verificação automática: " + e.getMessage()));
        } catch (Exception e) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                "Erro", "Erro inesperado ao executar verificação automática: " + e.getMessage()));
        }
    }
    
    public int getQuantidadeAgendamentosPendentesConclusao() {
        try {
            Collection<Agenda> agendamentosPendentes = agendaService.buscarAgendamentosParaConclusaoAutomatica();
            return agendamentosPendentes.size();
        } catch (Exception e) {
            System.err.println("Erro ao buscar agendamentos pendentes: " + e.getMessage());
            return 0;
        }
    }

    public boolean isHáAgendamentosPendentesConclusao() {
        return getQuantidadeAgendamentosPendentesConclusao() > 0;
    }

    public boolean isSistemaAtualizacaoAtivo() {
    	Usuario usuarioLogado = getUsuarioLogado();
        if (usuarioLogado != null) {
            return usuarioLogado.isAtualizacaoAutomaticaAtiva();
        }
        return true;
    }
    
    /**
     * Verifica se o sistema de atualização automática está ativo para o usuário atual
     * @return true se o usuário atual tem o sistema ativo
     */
    public boolean isSistemaAtivoParaUsuarioAtual() {
        Usuario usuarioLogado = getUsuarioLogado();
        if (usuarioLogado == null) {
            return false;
        }
        return sessionManager.isUsuarioAtivo(usuarioLogado.getId());
    }
    
    /**
     * Retorna informações sobre o estado do sistema de atualização automática
     * @return String com informações sobre usuários ativos
     */
    public String getInfoSistemaAtualizacao() {
        int usuariosAtivos = sessionManager.getQuantidadeUsuariosAtivos();
        boolean schedulerAtivo = sessionManager.isSchedulerAtivo();
        
        if (usuariosAtivos == 0) {
            return "Sistema desativado - Nenhum usuário ativo";
        } else if (usuariosAtivos == 1) {
            return "Sistema ativo - 1 usuário ativo";
        } else {
            return "Sistema ativo - " + usuariosAtivos + " usuários ativos";
        }
    }
    
    private void salvarEstadoSistema(boolean estado) {
        Map<String, Object> sessionMap = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
        sessionMap.put("sistemaAtualizacaoAtivo", estado);
        this.sistemaAtualizacaoAtivo = estado;
    }
    
    public void toggleSistemaAtualizacao() {
        boolean novoEstado = !isSistemaAtualizacaoAtivo();
        Usuario usuarioLogado = getUsuarioLogado();
        
        if (usuarioLogado != null) {
            usuarioLogado.setAtualizacaoAutomaticaAtiva(novoEstado);
            try {
                usuarioService.alterar(usuarioLogado); // salva no banco
            } catch (br.com.cesarsants.exceptions.TipoChaveNaoEncontradaException | br.com.cesarsants.exceptions.DAOException e) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro ao salvar preferência", e.getMessage()));
            }
        }
        System.out.println("==========================================");
        System.out.println("Novo estado do sistema: " + (novoEstado ? "ATIVO" : "INATIVO"));
        System.out.println("==========================================");
        // Obtém o usuário logado
        usuarioLogado = getUsuarioLogado();
        if (usuarioLogado == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Usuário não autenticado"));
            return;
        }
        
        if (novoEstado) {
            // Adiciona o usuário ao conjunto de usuários ativos
            sessionManager.adicionarUsuarioAtivo(usuarioLogado.getId());
            
            // Inicia o scheduler para o polling (apenas para este usuário)
            if (scheduler == null || scheduler.isShutdown()) {
                scheduler = Executors.newSingleThreadScheduledExecutor();
                scheduler.scheduleAtFixedRate(() -> {
                    verificarMudancasEAtualizar();
                }, 0, 1, TimeUnit.MINUTES);
            }
        } else {
            // Remove o usuário do conjunto de usuários ativos
            sessionManager.removerUsuarioAtivo(usuarioLogado.getId());
            
            // Para o scheduler do polling (apenas para este usuário)
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    scheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                scheduler = null;
            }
        }
        
        if (FacesContext.getCurrentInstance() != null) {
            PrimeFaces.current().ajax().update("toggleForm:toggleButton toggleForm:statusMessage pollingForm");
            
            if (novoEstado) {
                PrimeFaces.current().executeScript("PF('poll').start();");
            } else {
                PrimeFaces.current().executeScript("PF('poll').stop();");
            }
        }
    }

    @PreDestroy
    public void destroy() {
        // Remove o usuário do conjunto de usuários ativos
        Usuario usuarioLogado = getUsuarioLogado();
        if (usuarioLogado != null) {
            sessionManager.removerUsuarioAtivo(usuarioLogado.getId());
        }
        
        // Para o scheduler do polling
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

     public void clearSearch() {
        this.searchText = null;
        this.searchDate = null;
        this.searchTime = null;
        this.agendasFiltradas = null;
        this.ordenacao = "data_recente";
        refreshAgendamentos();
    }
    
    public void onSearchEntityTypeChange() {
        this.searchType = "nome";
        clearSearch();
    }
    
    public void filterAgendamentos() {
        try {
            Collection<Agenda> todosAgendamentos = agendaService.buscarTodosComRelacionamentos();
            
            if ((searchText == null || searchText.trim().isEmpty()) && 
                searchDate == null && searchTime == null) {
                this.agendasFiltradas = null;
                refreshAgendamentos();
                return;
            }
            
            this.agendasFiltradas = todosAgendamentos.stream()
                .filter(agenda -> {
                    if ("data".equals(searchEntityType)) {
                        if (searchDate != null && searchTime != null) {
                            LocalDateTime searchDateTime = LocalDateTime.of(searchDate, searchTime);
                            return agenda.getDataHora().equals(searchDateTime);
                        } else if (searchDate != null) {
                            return agenda.getDataHora().toLocalDate().equals(searchDate);
                        } else if (searchTime != null) {
                            return agenda.getDataHora().toLocalTime().equals(searchTime);
                        }
                        return true;
                    } else if ("paciente".equals(searchEntityType)) {
                        if (searchText == null || searchText.trim().isEmpty()) return true;
                        
                        if ("nome".equals(searchType)) {
                            return agenda.getPaciente().getNome().toLowerCase()
                                .contains(searchText.toLowerCase());
                        } else if ("id".equals(searchType)) {
                            try {
                                Long id = Long.parseLong(searchText);
                                return agenda.getPaciente().getId().equals(id);
                            } catch (NumberFormatException e) {
                                return false;
                            }
                        } else if ("cpf".equals(searchType)) {
                            if (agenda.getPaciente().getCpf() == null) return false;
                            String cpfSearch = searchText.replaceAll("[^0-9]", "");
                            String cpfValue = String.valueOf(agenda.getPaciente().getCpf());
                            return cpfValue.contains(cpfSearch);
                        } else if ("telefone".equals(searchType)) {
                            if (agenda.getPaciente().getTelefone() == null) return false;
                            String telSearch = searchText.replaceAll("[^0-9]", "");
                            String telValue = String.valueOf(agenda.getPaciente().getTelefone());
                            return telValue.contains(telSearch);
                        } else if ("endereco".equals(searchType)) {
                            return agenda.getPaciente().getEndereco().toLowerCase()
                                .contains(searchText.toLowerCase());
                        }
                    } else if ("medico".equals(searchEntityType)) {
                        if (searchText == null || searchText.trim().isEmpty()) return true;
                        
                        if ("nome".equals(searchType)) {
                            return agenda.getMedico().getNome().toLowerCase()
                                .contains(searchText.toLowerCase());
                        } else if ("id".equals(searchType)) {
                            try {
                                Long id = Long.parseLong(searchText);
                                return agenda.getMedico().getId().equals(id);
                            } catch (NumberFormatException e) {
                                return false;
                            }
                        } else if ("cpf".equals(searchType)) {
                            if (agenda.getMedico().getCpf() == null) return false;
                            String cpfSearch = searchText.replaceAll("[^0-9]", "");
                            String cpfValue = String.valueOf(agenda.getMedico().getCpf());
                            return cpfValue.contains(cpfSearch);
                        } else if ("endereco".equals(searchType)) {
                            return agenda.getMedico().getEndereco().toLowerCase()
                                .contains(searchText.toLowerCase());
                        }
                    } else if ("clinica".equals(searchEntityType)) {
                        if (searchText == null || searchText.trim().isEmpty()) return true;
                        
                        if ("nome".equals(searchType)) {
                            return agenda.getClinica().getNome().toLowerCase()
                                .contains(searchText.toLowerCase());
                        } else if ("id".equals(searchType)) {
                            try {
                                Long id = Long.parseLong(searchText);
                                return agenda.getClinica().getId().equals(id);
                            } catch (NumberFormatException e) {
                                return false;
                            }
                        } else if ("cnpj".equals(searchType)) {
                            if (agenda.getClinica().getCnpj() == null) return false;
                            String cnpjSearch = searchText.replaceAll("[^0-9]", "");
                            String cnpjValue = String.valueOf(agenda.getClinica().getCnpj());
                            return cnpjValue.contains(cnpjSearch);
                        } else if ("endereco".equals(searchType)) {
                            return agenda.getClinica().getEndereco().toLowerCase()
                                .contains(searchText.toLowerCase());
                        } else if ("horario".equals(searchType)) {
                            if (searchTime == null) return true;
                            return agenda.getClinica().getHorarioAbertura().equals(searchTime) ||
                                   agenda.getClinica().getHorarioFechamento().equals(searchTime);
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
                
            this.agendasFiltradas = aplicarOrdenacao(new ArrayList<>(this.agendasFiltradas));
            
            if (this.agendasFiltradas != null && !this.agendasFiltradas.isEmpty()) {
                Map<Long, Collection<Sala>> salasClinicaCache = new HashMap<>();
                
                for (Agenda agenda : this.agendasFiltradas) {
                    if (agenda.getSala() == null) {
                        try {
                            // Usa cache para evitar buscar as salas da mesma clínica várias vezes
                            Collection<Sala> salasClinica = salasClinicaCache.computeIfAbsent(
                                agenda.getClinica().getId(),
                                clinicaId -> {
                                    try {
                                        return salaService.buscarPorClinica(clinicaId);
                                    } catch (Exception e) {
                                        System.err.println("Erro ao buscar salas da clínica " + clinicaId + ": " + e.getMessage());
                                        return Collections.emptyList();
                                    }
                                }
                            );
                            
                            // Busca a sala pelo número/ordem
                            Sala sala = salasClinica.stream()
                                .filter(s -> s.getOrdem().equals(agenda.getNumeroSala()))
                                .findFirst()
                                .orElse(null);
                            
                            if (sala != null) {
                                agenda.setSala(sala);
                            }
                            
                        } catch (Exception ex) {
                            System.err.println("Erro ao carregar sala para agendamento filtrado " + agenda.getId() + 
                                             " (Clínica: " + agenda.getClinica().getId() + 
                                             ", Sala: " + agenda.getNumeroSala() + "): " + ex.getMessage());
                        }
                    }
                }
            }
                
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", 
                    "Erro ao filtrar agendamentos: " + e.getMessage()));
        }
    }

    private List<Agenda> aplicarOrdenacao(List<Agenda> agendamentos) {
        if (agendamentos == null || agendamentos.isEmpty()) {
            return agendamentos;
        }
        
        switch (ordenacao) {
            case "data_antiga":
                return agendamentos.stream()
                    .sorted((a1, a2) -> a1.getDataHora().compareTo(a2.getDataHora()))
                    .collect(Collectors.toList());
                    
            case "clinica":
                return agendamentos.stream()
                    .sorted((a1, a2) -> {
                        // Primeiro ordena por nome da clínica
                        int comparacaoClinica = a1.getClinica().getNome().compareTo(a2.getClinica().getNome());
                        if (comparacaoClinica != 0) {
                            return comparacaoClinica;
                        }
                        // Se for a mesma clínica, ordena por data mais recente primeiro
                        return a2.getDataHora().compareTo(a1.getDataHora());
                    })
                    .collect(Collectors.toList());
                    
            case "medico":
                return agendamentos.stream()
                    .sorted((a1, a2) -> {
                        // Primeiro ordena por nome do médico
                        int comparacaoMedico = a1.getMedico().getNome().compareTo(a2.getMedico().getNome());
                        if (comparacaoMedico != 0) {
                            return comparacaoMedico;
                        }
                        // Se for o mesmo médico, ordena por data mais recente primeiro
                        return a2.getDataHora().compareTo(a1.getDataHora());
                    })
                    .collect(Collectors.toList());
                    
            case "paciente":
                return agendamentos.stream()
                    .sorted((a1, a2) -> {
                        // Primeiro ordena por nome do paciente
                        int comparacaoPaciente = a1.getPaciente().getNome().compareTo(a2.getPaciente().getNome());
                        if (comparacaoPaciente != 0) {
                            return comparacaoPaciente;
                        }
                        // Se for o mesmo paciente, ordena por data mais recente primeiro
                        return a2.getDataHora().compareTo(a1.getDataHora());
                    })
                    .collect(Collectors.toList());
                    
            case "data_recente":
            default:
                return agendamentos.stream()
                    .sorted((a1, a2) -> a2.getDataHora().compareTo(a1.getDataHora()))
                    .collect(Collectors.toList());
        }
    }

    public void onOrdenacaoChange() {
        if (this.agendasFiltradas != null) {
            // Se há filtros ativos, reaplica a ordenação na lista filtrada
            this.agendasFiltradas = aplicarOrdenacao(new ArrayList<>(this.agendasFiltradas));
        } else {
            // Se não há filtros, recarrega a lista padrão com a nova ordenação
            refreshAgendamentos();
        }
    }
    

// Getters e Setters 
    
    public Medico getSelectedMedico() {
        return selectedMedico;
    }
    
    public void setSelectedMedico(Medico selectedMedico) {
        this.selectedMedico = selectedMedico;
        updateDynamicMedico();
    }

    public Long getSelectedClinicaId() {
        return selectedClinicaId;
    }

    public void setSelectedClinicaId(Long selectedClinicaId) {
        this.selectedClinicaId = selectedClinicaId;
    } 

    public Sala getSelectedSala() {
        return selectedSala;
    }

    public void setSelectedSala(Sala selectedSala) {
        this.selectedSala = selectedSala;
        updateDynamicSala();
    }

    public LocalDateTime getSelectedData() {
        return selectedData;
    }

    public void setSelectedData(LocalDateTime selectedData) {
        this.selectedData = selectedData;
    }

    public LocalTime getSelectedHorario() {
        return selectedHorario;
    }

    public void setSelectedHorario(LocalTime selectedHorario) {
        this.selectedHorario = selectedHorario;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public Paciente getSelectedPaciente() {
        return selectedPaciente;
    }

    public void setSelectedPaciente(Paciente selectedPaciente) {
        this.selectedPaciente = selectedPaciente;
    }

    public Agenda getSelectedConsulta() {
        return selectedConsulta;
    }

    public void setSelectedConsulta(Agenda selectedConsulta) {
        this.selectedConsulta = selectedConsulta;
    }

    public List<Agenda> getConsultasAgendadas() {
        if (this.agendasFiltradas != null) {
        	refreshAgendamentos();
            return new ArrayList<>(this.agendasFiltradas);
            
        }
        if (this.consultasAgendadas == null) {
            refreshAgendamentos();
        }
        return this.consultasAgendadas;
    }

    public Collection<Clinica> getClinicas() {
        return clinicas;
    }

    public Collection<Medico> getMedicos() {
        return medicos;
    }

    public Collection<Paciente> getPacientes() {
        return pacientes;
    }

    public Collection<Agenda> getAgendamentos() {
        return agendamentos;
    }

    public Boolean getIsUpdate() {
        return isUpdate;
    }

    public Collection<Sala> getSalasDisponiveis() {
        if (selectedConsulta == null) {
            return Collections.emptyList();
        }
        try {
            Collection<Sala> salasDisponiveis = getSalasDisponiveisParaEdicao();
            return salasDisponiveis.stream()
                .sorted(Comparator.comparing(Sala::getOrdem))
                .collect(Collectors.toList());
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao buscar salas disponíveis"));
            return Collections.emptyList();
        }
    }

    public void setSalasDisponiveis(Collection<Sala> salasDisponiveis) {
        this.salasDisponiveis = salasDisponiveis;
    }

    //getters e setters para os campos de data e hora

    public LocalDate getToday() {
        return today;
    }

    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    public void setSelectedDate(LocalDate selectedDate) {
        this.selectedDate = selectedDate;
        updateDynamicDateTime();
        
        if (selectedDate != null && selectedTime != null) {
            try {
                updateSelectedDateTime();
            } catch (Exception e) {
                System.err.println("Error validating date: " + e.getMessage());
            }
        }
    }

    public LocalTime getSelectedTime() {
        return selectedTime;
    }

    public void setSelectedTime(LocalTime selectedTime) {
        this.selectedTime = selectedTime;
        updateDynamicDateTime();
        
        if (selectedDate != null && selectedTime != null) {
            try {
                updateSelectedDateTime();
            } catch (Exception e) {
                System.err.println("Error validating time: " + e.getMessage());
            }
        }
    }

//getters e setters para os campos dinâmicos que serão atualizados via AJAX

    public String getDynamicDateTime() {
        return dynamicDateTime;
    }

    public void updateDynamicDateTime() {
        String dataStr = "";
        String horaStr = "";
        
        if (selectedTime != null) {
            try {
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
                horaStr = selectedTime.format(timeFormatter);
            } catch (Exception e) {
                horaStr = "";
            }
        }
        
        if (selectedDate != null) {
            try {
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                dataStr = selectedDate.format(dateFormatter);
            } catch (Exception e) {
                dataStr = "";
            }
        }
        
        if (!dataStr.isEmpty() || !horaStr.isEmpty()) {
            this.dynamicDateTime = (dataStr + " " + horaStr).trim();
        } else {
            this.dynamicDateTime = "";
        }
        
        if (FacesContext.getCurrentInstance() != null) {
            PrimeFaces.current().ajax().update(":datetimeForm:datetime-panel");
        }
    } 

    private void updateSelectedDateTime() {
        if (selectedDate != null && selectedTime != null) {
            this.selectedData = LocalDateTime.of(selectedDate, selectedTime);
        } else {
            this.selectedData = null;
        }
    }

    public String getDynamicSelectedMedico() {
        return dynamicSelectedMedico;
    }

    public void updateDynamicMedico() {
        if (selectedMedico != null) {
            this.dynamicSelectedMedico = selectedMedico.getNome();
        } else {
            this.dynamicSelectedMedico = "";
        }
    }

    public String getDynamicSelectedClinica() {
        return dynamicSelectedClinica;
    }

    public void updateDynamicClinica() {
        if (selectedClinicaId != null) {
            try {
                Clinica clinica = clinicaService.consultar(selectedClinicaId);
                if (clinica != null) {
                    this.dynamicSelectedClinica = clinica.getNome();
                } else {
                    this.dynamicSelectedClinica = "";
                }
            } catch (Exception e) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao buscar clínica: " + e.getMessage()));
                this.dynamicSelectedClinica = "";
            }
        } else {
            this.dynamicSelectedClinica = "";
        }
    }  

     public void updateDynamicSala() {
        if (isUpdate) {
            if (editSala != null) {
                this.dynamicSelectedSala = editSala.getNome();
                System.out.println("UpdateDynamicSala - Sala editada: " + editSala.getNome() + 
                                 " (ID: " + editSala.getId() + ", Ordem: " + editSala.getOrdem() + ")");
            } 
            else if (salaAtual != null) {
                this.dynamicSelectedSala = salaAtual.getNome();
                System.out.println("UpdateDynamicSala - Usando sala atual: " + salaAtual.getNome() + 
                                 " (ID: " + salaAtual.getId() + ", Ordem: " + salaAtual.getOrdem() + ")");
            }
            else {
                this.dynamicSelectedSala = "";
                System.out.println("UpdateDynamicSala - Nenhuma sala selecionada");
            }
            
            if (FacesContext.getCurrentInstance() != null) {
                PrimeFaces.current().ajax().update(":editForm:editSalaId");
                PrimeFaces.current().ajax().update(":datetimeForm:sala-panel");
                System.out.println("UpdateDynamicSala - UI atualizada com valor: " + this.dynamicSelectedSala);
            }
        } else {
            if (selectedSala != null) {
                this.dynamicSelectedSala = selectedSala.getNome();
            } else {
                this.dynamicSelectedSala = "";
            }
        }
    }

    public void updateDynamicPaciente() {
        if (selectedPaciente != null) {
            this.dynamicSelectedPaciente = selectedPaciente.getNome();
        } else {
            this.dynamicSelectedPaciente = "";
        }
    }

    public String getDynamicSelectedPaciente() {
        return dynamicSelectedPaciente;
    }

    public String getDynamicSelectedSala() {
        return dynamicSelectedSala;
    } 

//getters e setters para os nomes dos médicos, pacientes e salas indisponíveis, usado no formulario de cadastro

    public List<String> getMedicosIndisponiveisNomes() {
        return medicosIndisponiveisNomes;
    }

    public void setMedicosIndisponiveisNomes(List<String> medicosIndisponiveisNomes) {
        this.medicosIndisponiveisNomes = medicosIndisponiveisNomes;
    }

   public List<String> getPacientesIndisponiveisNomes() {
        return pacientesIndisponiveisNomes;
    }

    public void setPacientesIndisponiveisNomes(List<String> pacientesIndisponiveisNomes) {
        this.pacientesIndisponiveisNomes = pacientesIndisponiveisNomes;
    }

        public List<String> getSalasIndisponiveisNomes() {
        return salasIndisponiveisNomes;
    }

    public void setSalasIndisponiveisNomes(List<String> salasIndisponiveisNomes) {
        this.salasIndisponiveisNomes = salasIndisponiveisNomes;
    }

    public Collection<Sala> getSalasIndisponiveis() {
        if (salasIndisponiveis == null) {
            salasIndisponiveis = new ArrayList<>();
        }
        return salasIndisponiveis.stream()
            .sorted(Comparator.comparing(Sala::getOrdem))
            .collect(Collectors.toList());
    }

    public void setSalasIndisponiveis(Collection<Sala> salasIndisponiveis) {
        this.salasIndisponiveis = salasIndisponiveis;
    }   

    // Getters e Setters campos de edição
    public Medico getEditMedico() {
        return editMedico;
    }
    
    public void setEditMedico(Medico editMedico) {
        this.editMedico = editMedico;
    }
    
    public Sala getEditSala() {
        return editSala;
    }
      public void setEditSala(Sala editSala) {
        System.out.println("SetEditSala chamado com sala: " + 
                         (editSala != null ? editSala.getNome() + " (ID: " + editSala.getId() + ", Ordem: " + editSala.getOrdem() + ")" : "null"));
        this.editSala = editSala;
        updateDynamicEditSala();
    }
    
    public String getEditObservacoes() {
        return editObservacoes;
    }
    
    public void setEditObservacoes(String editObservacoes) {
        this.editObservacoes = editObservacoes;
    }
    
    public LocalDateTime getEditDataHora() {
        return editDataHora;
    }
    
    public void setEditDataHora(LocalDateTime editDataHora) {
        this.editDataHora = editDataHora;
    }

    
    public String getDynamicEditDateTime() {
        return dynamicEditDateTime;
    }

    public void setDynamicEditDateTime(String dynamicEditDateTime) {
        this.dynamicEditDateTime = dynamicEditDateTime;
    }

    // getters e setters para pesquisa

    public String getSearchEntityType() {
        return searchEntityType;
    }
    
    public void setSearchEntityType(String searchEntityType) {
        this.searchEntityType = searchEntityType;
    }
    
    public String getSearchType() {
        return searchType;
    }
    
    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }
    
    public String getSearchText() {
        return searchText;
    }
    
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }
    
    public LocalDate getSearchDate() {
        return searchDate;
    }
    
    public void setSearchDate(LocalDate searchDate) {
        this.searchDate = searchDate;
    }
    
    public LocalTime getSearchTime() {
        return searchTime;
    }
    
    public void setSearchTime(LocalTime searchTime) {
        this.searchTime = searchTime;
    }

    public Collection<Agenda> getAgendasFiltradas() {
        return agendasFiltradas != null ? agendasFiltradas : getConsultasAgendadas();
    }
    
    // Getters e Setters para ordenação
    public String getOrdenacao() {
        return ordenacao;
    }
    
    public void setOrdenacao(String ordenacao) {
        this.ordenacao = ordenacao;
    }
}