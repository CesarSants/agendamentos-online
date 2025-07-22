package br.com.cesarsants.controller;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import br.com.cesarsants.domain.Clinica;
import br.com.cesarsants.domain.Medico;
import br.com.cesarsants.domain.Sala;
import br.com.cesarsants.domain.Usuario;
import br.com.cesarsants.exceptions.DAOException;
import br.com.cesarsants.exceptions.MaisDeUmRegistroException;
import br.com.cesarsants.exceptions.TableException;
import br.com.cesarsants.exceptions.TipoChaveNaoEncontradaException;
import br.com.cesarsants.service.IClinicaService;
import br.com.cesarsants.service.MedicoService;

@ManagedBean(name = "clinicaController")
@ViewScoped
public class ClinicaController implements Serializable {

    private static final long serialVersionUID = 1L;
    
    // Propriedades principais
    private Clinica clinica;
    private Collection<Clinica> clinicas;
    private Collection<Clinica> clinicasExibidas;
    private Collection<Medico> medicosDisponiveis;
    private List<Medico> selectedMedicos;
    private Clinica selectedClinica;
    private Boolean isUpdate;
    private String cnpjMask;
    
    // Propriedades de busca principal
    private String searchText;
    private String searchType = "nome"; // valor padrão

    // Propriedades de busca para médicos já vinculados
    private String searchTextMedicos;
    private String searchTypeMedicos = "nome"; // valor padrão
    private Collection<Medico> medicosExibidos;

    // Propriedades de busca para médicos disponíveis
    private String searchTextDisponiveis;
    private String searchTypeDisponiveis = "nome"; // valor padrão
    private Collection<Medico> medicosDisponiveisExibidos;
    
    private Sala selectedSala;
    
    private final IClinicaService clinicaService = new br.com.cesarsants.service.ClinicaService();
    
    private MedicoService medicoService = new MedicoService();

    // Novo campo para busca por horário
    private LocalTime searchTime;
    
    @PostConstruct
    public void init() {
        try {
            this.clinicas = clinicaService.buscarTodos();
            this.medicosDisponiveis = medicoService.buscarTodos();
            this.isUpdate = false;
            this.clinica = new Clinica();
            this.selectedClinica = new Clinica();
            this.clinicasExibidas = this.clinicas;  // Initialize clinicasExibidas
            this.searchType = "nome";  // Set default search type
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage()));
        }
    }

    public void cancel() {
        this.clinica = new Clinica();
        this.isUpdate = false;
    }
    
    public void edit(Clinica clinica) {
        try {
            this.isUpdate = true;
            // Clona o objeto para edição
            this.clinica = new Clinica();
            this.clinica.setId(clinica.getId());
            this.clinica.setNome(clinica.getNome());
            this.clinica.setCnpj(clinica.getCnpj());
            this.clinica.setEndereco(clinica.getEndereco());
            this.clinica.setHorarioAbertura(clinica.getHorarioAbertura());
            this.clinica.setHorarioFechamento(clinica.getHorarioFechamento());
            this.clinica.setNumeroTotalSalas(clinica.getNumeroTotalSalas());
            this.clinica.setUsuario(clinica.getUsuario());
            this.cnpjMask = clinica.getCnpj() != null ? clinica.getCnpj().toString() : "";
        } catch(Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage()));
        }
    }
    
    public void delete(Clinica clinica) {
        try {
            clinicaService.excluir(clinica);
            this.clinicas.remove(clinica);
            this.clinicasExibidas.remove(clinica);
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Clínica excluída com sucesso"));
        } catch (DAOException e) {
            // Exibe a mensagem personalizada do service
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Não foi possível excluir a clínica", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace(); // Log do erro para debug
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro inesperado", "Ocorreu um erro ao tentar excluir a clínica"));
        }
    }
    
    public void save() {
        try {
            // Associa o usuário logado à clínica
            Usuario usuarioLogado = getUsuarioLogado();
            if (usuarioLogado == null) {
                throw new Exception("Usuário não autenticado");
            }
            this.clinica.setUsuario(usuarioLogado);
            
            // Validação de CNPJ único - apenas verifica se existe, não reutiliza o objeto
            Clinica clinicaDb = clinicaService.buscarPorCNPJ(this.clinica.getCnpj(), usuarioLogado);
            if (clinicaDb != null && (this.clinica.getId() == null || !clinicaDb.getId().equals(this.clinica.getId()))) {
                throw new Exception("CNPJ já cadastrado");
            }
            
            if (this.isUpdate) {
                clinicaService.alterar(this.clinica);
            } else {
                clinicaService.cadastrar(this.clinica);
            }
            
            // Atualiza a lista de clínicas e limpa o formulário
            this.clinicas = clinicaService.buscarTodos();
            this.clinicasExibidas = this.clinicas; // Update filtered list too
            this.clinica = new Clinica();
            this.isUpdate = false;
            
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Clínica salva com sucesso"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage()));
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

    public void filterMedicos() {
        try {
            // Recarrega a clínica para garantir dados atualizados
            if (selectedClinica != null && selectedClinica.getId() != null) {
                this.selectedClinica = clinicaService.consultar(selectedClinica.getId());
                if (searchText == null || searchText.trim().isEmpty()) {
                    this.medicosExibidos = this.selectedClinica.getMedicos();
                    return;
                }

                this.medicosExibidos = this.selectedClinica.getMedicos().stream()
                    .filter(medico -> {
                        if ("nome".equals(searchType)) {
                            return medico.getNome().toLowerCase().contains(searchText.toLowerCase());
                        } else { // id
                            try {
                                if (searchText.matches("\\d+")) {
                                    Long searchId = Long.parseLong(searchText);
                                    return medico.getId().equals(searchId);
                                }
                            } catch (NumberFormatException e) {
                                // Ignora erro de conversão
                            }
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao filtrar médicos: " + e.getMessage()));
        }
    }    public void vincularMedicos() {
        try {
            if (selectedMedicos != null && !selectedMedicos.isEmpty()) {
                for (Medico medico : selectedMedicos) {
                    clinicaService.adicionarMedico(selectedClinica.getId(), medico.getId());
                }
                
                // Atualiza os dados
                this.selectedClinica = clinicaService.consultar(selectedClinica.getId());
                this.clinicas = clinicaService.buscarTodos();
                // Atualiza a lista exibida também para refletir o novo número de médicos
                this.clinicasExibidas = this.clinicas;
                this.medicosDisponiveis = medicoService.buscarTodos();
                if (this.selectedClinica != null && this.selectedClinica.getMedicos() != null) {
                    this.medicosDisponiveis.removeAll(this.selectedClinica.getMedicos());
                    this.medicosDisponiveisExibidos = this.medicosDisponiveis;
                    this.medicosExibidos = this.selectedClinica.getMedicos();
                }
                
                // Limpa seleção e busca
                this.selectedMedicos = null;
                this.searchText = "";
                this.searchTextDisponiveis = "";
                this.searchType = "nome";
                this.searchTypeDisponiveis = "nome";
                
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Médicos vinculados com sucesso!"));
            } else {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Aviso", "Nenhum médico selecionado para vincular."));
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Falha ao vincular médicos: " + e.getMessage()));
        }
    }    public void desvincularMedico(Medico medico) {
        try {
            clinicaService.removerMedicoComContexto(selectedClinica.getId(), medico.getId());
            
            // Atualiza os dados
            this.selectedClinica = clinicaService.consultar(selectedClinica.getId());
            this.clinicas = clinicaService.buscarTodos();
            // Atualiza a lista exibida também para refletir o novo número de médicos
            this.clinicasExibidas = this.clinicas;
            this.medicosDisponiveis = medicoService.buscarTodos();
            if (this.selectedClinica != null && this.selectedClinica.getMedicos() != null) {
                this.medicosDisponiveis.removeAll(this.selectedClinica.getMedicos());
                this.medicosDisponiveisExibidos = this.medicosDisponiveis;
                this.medicosExibidos = this.selectedClinica.getMedicos();
            }
            
            // Limpa as buscas
            this.searchText = "";
            this.searchTextDisponiveis = "";
            this.searchType = "nome";
            this.searchTypeDisponiveis = "nome";
            
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Médico desvinculado com sucesso"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Falha ao desvincular médico: " + e.getMessage()));
        }
    }

    public void filterClinicas() {
        try {
            if (this.clinicas == null) {
                this.clinicas = clinicaService.buscarTodos();
            }

            if ((searchText == null || searchText.trim().isEmpty()) && searchTime == null) {
                this.clinicasExibidas = this.clinicas;
                return;
            }

            this.clinicasExibidas = this.clinicas.stream()
                .filter(clinica -> {
                    if ("horario".equals(searchType) && searchTime != null) {
                        // Caso 1: Horário de abertura menor que horário de fechamento (Ex: 09:00 - 18:00)
                        if (clinica.getHorarioAbertura().isBefore(clinica.getHorarioFechamento())) {
                            return searchTime.compareTo(clinica.getHorarioAbertura()) >= 0 &&
                                   searchTime.compareTo(clinica.getHorarioFechamento()) <= 0;
                        }
                        // Caso 2: Horário de abertura igual ao horário de fechamento (24h)
                        else if (clinica.getHorarioAbertura().equals(clinica.getHorarioFechamento())) {
                            return true;
                        }
                        // Caso 3: Horário de abertura maior que horário de fechamento (Ex: 20:00 - 10:00)
                        else {
                            return searchTime.compareTo(clinica.getHorarioAbertura()) >= 0 ||
                                   searchTime.compareTo(clinica.getHorarioFechamento()) <= 0;
                        }
                    } else if ("nome".equals(searchType)) {
                        return clinica.getNome().toLowerCase().contains(searchText.toLowerCase());
                    } else if ("cnpj".equals(searchType)) {
                        String cnpjString = clinica.getCnpj().toString();
                        return cnpjString.contains(searchText.replaceAll("[^0-9]", ""));
                    } else { // endereco
                        return clinica.getEndereco().toLowerCase().contains(searchText.toLowerCase());
                    }
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            this.clinicasExibidas = Collections.emptyList();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao filtrar clínicas: " + e.getMessage()));
        }
    }    public void handleSearchTypeChange(javax.faces.event.AjaxBehaviorEvent event) {
        this.searchText = "";
        this.searchTime = null;
        filterClinicas();
    }

    public void clearSearch() {
        this.searchText = "";
        this.searchTime = null;
//        this.searchType = "nome";
        this.clinicasExibidas = this.clinicas;
    }

    public void setSelectedClinica(Clinica clinica) {
        try {
            this.selectedClinica = null;
            if (clinica != null && clinica.getId() != null) {
                // Clona o objeto para evitar problemas com entidades gerenciadas
                this.selectedClinica = new Clinica();
                this.selectedClinica.setId(clinica.getId());
                this.selectedClinica.setNome(clinica.getNome());
                this.selectedClinica.setCnpj(clinica.getCnpj());
                this.selectedClinica.setEndereco(clinica.getEndereco());
                this.selectedClinica.setHorarioAbertura(clinica.getHorarioAbertura());
                this.selectedClinica.setHorarioFechamento(clinica.getHorarioFechamento());
                this.selectedClinica.setUsuario(clinica.getUsuario());
                this.selectedClinica.setSalas(clinica.getSalas());
                
                // Initialize rooms if needed, preserving existing orders
                if (this.selectedClinica.getSalas() == null || this.selectedClinica.getSalas().isEmpty()) {
                    this.selectedClinica.initializeSalas();
                    clinicaService.alterar(this.selectedClinica);
                    // Recarrega a clínica após a alteração
                    Clinica clinicaRecarregada = clinicaService.consultar(clinica.getId());
                    this.selectedClinica.setSalas(clinicaRecarregada.getSalas());
                }
                
                // Handle medicos - carrega os médicos da clínica original
                this.medicosDisponiveis = medicoService.buscarTodos();
                Set<Medico> medicosClinica = clinica.getMedicos();
                if (this.selectedClinica != null && medicosClinica != null) {
                    this.medicosDisponiveis.removeAll(medicosClinica);
                    this.medicosDisponiveisExibidos = this.medicosDisponiveis;
                    this.medicosExibidos = medicosClinica;
                    this.searchText = "";
                    this.searchType = "nome";
                    this.searchTextDisponiveis = "";
                    this.searchTypeDisponiveis = "nome";
                }
            }
        } catch (Exception e) {
            this.selectedClinica = new Clinica();
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao selecionar clínica: " + e.getMessage()));
        }
    }    public Collection<Medico> getMedicosExibidos() {
        if (medicosExibidos == null) {
            try {
                if (selectedClinica != null && selectedClinica.getId() != null) {
                    this.selectedClinica = clinicaService.consultar(selectedClinica.getId());
                    this.medicosExibidos = this.selectedClinica.getMedicos();
                } else {
                    this.medicosExibidos = Collections.emptyList();
                }
            } catch (Exception e) {
                this.medicosExibidos = Collections.emptyList();
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao carregar médicos: " + e.getMessage()));
            }
        }
        return this.medicosExibidos;
    }

    public List<Medico> getSelectedMedicos() {
        return selectedMedicos;
    }

    public void setSelectedMedicos(List<Medico> selectedMedicos) {
        this.selectedMedicos = selectedMedicos;
    }

    public Clinica getSelectedClinica() {
        return selectedClinica;
    }


    public String formatarCnpj(Long cnpj) {
        if (cnpj == null) return "";
        String cnpjStr = String.format("%014d", cnpj);
        return cnpjStr.substring(0, 2) + "." + 
               cnpjStr.substring(2, 5) + "." + 
               cnpjStr.substring(5, 8) + "/" + 
               cnpjStr.substring(8, 12) + "-" + 
               cnpjStr.substring(12, 14);
    }
    
    // Getters e Setters

    public Clinica getClinica() {
        return clinica;
    }

    public void setClinica(Clinica clinica) {
        this.clinica = clinica;
    }

    public Collection<Clinica> getClinicas() {
        return clinicas;
    }

    public void setClinicas(Collection<Clinica> clinicas) {
        this.clinicas = clinicas;
    }

    public Collection<Medico> getMedicosDisponiveis() {
        return medicosDisponiveis;
    }

    public void setMedicosDisponiveis(Collection<Medico> medicosDisponiveis) {
        this.medicosDisponiveis = medicosDisponiveis;
    }

    public Boolean getIsUpdate() {
        return isUpdate;
    }

    public void setIsUpdate(Boolean isUpdate) {
        this.isUpdate = isUpdate;
    }

    public String getCnpjMask() {
        return cnpjMask;
    }

    public void setCnpjMask(String cnpjMask) {
        if (cnpjMask != null && !cnpjMask.isEmpty()) {
            this.cnpjMask = cnpjMask.replaceAll("[^0-9]", "");
            this.clinica.setCnpj(Long.valueOf(this.cnpjMask));
        }
    }

    public void filterMedicosDisponiveis() {
        try {
            if (medicosDisponiveis != null) {
                if (searchTextDisponiveis == null || searchTextDisponiveis.trim().isEmpty()) {
                    this.medicosDisponiveisExibidos = this.medicosDisponiveis;
                    return;
                }

                this.medicosDisponiveisExibidos = this.medicosDisponiveis.stream()
                    .filter(medico -> {
                        if ("nome".equals(searchTypeDisponiveis)) {
                            return medico.getNome().toLowerCase().contains(searchTextDisponiveis.toLowerCase());
                        } else { // id
                            try {
                                if (searchTextDisponiveis.matches("\\d+")) {
                                    Long searchId = Long.parseLong(searchTextDisponiveis);
                                    return medico.getId().equals(searchId);
                                }
                            } catch (NumberFormatException e) {
                                // Ignora erro de conversão
                            }
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao filtrar médicos disponíveis: " + e.getMessage()));
        }
    }

    public void clearSearchDisponiveis() {
        try {
            this.searchTextDisponiveis = "";
            this.medicosDisponiveisExibidos = this.medicosDisponiveis;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao limpar busca: " + e.getMessage()));
        }
    }

    public String getSearchTextDisponiveis() {
        return searchTextDisponiveis;
    }

    public void setSearchTextDisponiveis(String searchTextDisponiveis) {
        this.searchTextDisponiveis = searchTextDisponiveis;
    }

    public String getSearchTypeDisponiveis() {
        return searchTypeDisponiveis;
    }

    public void setSearchTypeDisponiveis(String searchTypeDisponiveis) {
        this.searchTypeDisponiveis = searchTypeDisponiveis;
    }

    public Collection<Medico> getMedicosDisponiveisExibidos() {
        if (medicosDisponiveisExibidos == null) {
            medicosDisponiveisExibidos = medicosDisponiveis;
        }
        return medicosDisponiveisExibidos;
    }

    public void setMedicosDisponiveisExibidos(Collection<Medico> medicosDisponiveisExibidos) {
        this.medicosDisponiveisExibidos = medicosDisponiveisExibidos;
    }    public void editSala(Sala sala) {
        try {
            // Create a new instance to avoid modifying the original directly
            this.selectedSala = new Sala();
            // Copy all properties, ensuring we keep the original ordem
            this.selectedSala.setId(sala.getId());
            this.selectedSala.setNome(sala.getNome());
            this.selectedSala.setOrdem(sala.getOrdem()); // Keep the original ordem to preserve order
            this.selectedSala.setClinica(sala.getClinica());
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao editar sala: " + e.getMessage()));
        }
    }    public void saveSala() {
        try {
            // First find the sala in the set by ID
            Sala salaToUpdate = selectedClinica.getSalas().stream()
                .filter(sala -> sala.getId().equals(selectedSala.getId()))
                .findFirst()
                .orElse(null);
                
            if (salaToUpdate != null) {
                // Store the original order
                Integer originalOrdem = salaToUpdate.getOrdem();
                
                // Update only the name
                salaToUpdate.setNome(selectedSala.getNome());
                
                // Ensure ordem is preserved
                salaToUpdate.setOrdem(originalOrdem);
                
                // Save changes
                clinicaService.alterar(selectedClinica);
                
                // Refresh the selected clinica to ensure correct order
                this.selectedClinica = clinicaService.consultar(selectedClinica.getId());
                
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Sala atualizada com sucesso"));
            } else {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Sala não encontrada"));
            }
        } catch (DAOException | TipoChaveNaoEncontradaException | MaisDeUmRegistroException | TableException e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao atualizar sala: " + e.getMessage()));
        }
    }

    public void updateSala(Sala salaToProcess) {
        try {
            // Find the clinica's latest state from the database
            Clinica currentClinica = clinicaService.consultar(salaToProcess.getClinica().getId());
            
            // Find the sala and preserve its order
            Sala salaToUpdate = currentClinica.getSalas().stream()
                .filter(s -> s.getId().equals(salaToProcess.getId()))
                .findFirst()
                .orElse(null);
            
            if (salaToUpdate != null) {
                // Store the original order
                Integer originalOrdem = salaToUpdate.getOrdem();
                
                // Update sala properties while preserving order
                salaToUpdate.setNome(salaToProcess.getNome());
                salaToUpdate.setOrdem(originalOrdem);
                
                // Save changes
                clinicaService.alterar(currentClinica);
                
                // Refresh the selected clinica if this update affects it
                if (this.selectedClinica != null && this.selectedClinica.getId().equals(currentClinica.getId())) {
                    this.selectedClinica = clinicaService.consultar(currentClinica.getId());
                }
                
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Sala atualizada com sucesso"));
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Sala não encontrada"));
            }
        } catch (DAOException | TipoChaveNaoEncontradaException | MaisDeUmRegistroException | TableException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao atualizar sala: " + e.getMessage()));
        }
    }

    public Sala getSelectedSala() {
        return selectedSala;
    }

    public void setSelectedSala(Sala selectedSala) {
        this.selectedSala = selectedSala;
    }

    public int sortByOrdem(Object obj1, Object obj2) {
        try {
            Sala sala1 = (Sala) obj1;
            Sala sala2 = (Sala) obj2;
            
            if (sala1.getOrdem() == null || sala2.getOrdem() == null) {
                return 0;
            }
            
            return sala1.getOrdem().compareTo(sala2.getOrdem());
        } catch (Exception e) {
            return 0;
        }
    }
    
    public List<Sala> getSalasOrdenadas() {
        if (selectedClinica != null && selectedClinica.getSalas() != null) {
            return selectedClinica.getSalas().stream()
                .sorted(Comparator.comparing(Sala::getOrdem))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    // Getters e Setters de busca
    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }

    public Collection<Clinica> getClinicasExibidas() {
        if (clinicasExibidas == null) {
            clinicasExibidas = clinicas;
        }
        return clinicasExibidas;
    }

    public void setClinicasExibidas(Collection<Clinica> clinicasExibidas) {
        this.clinicasExibidas = clinicasExibidas;
    }

    public LocalTime getSearchTime() {
        return searchTime;
    }

    public void setSearchTime(LocalTime searchTime) {
        this.searchTime = searchTime;
    }

    /**
     * Atualiza a lista de médicos disponíveis para vínculo, excluindo os já vinculados à clínica selecionada.
     */
    public void prepararMedicosDisponiveisParaVinculo() {
        try {
            // Busca todos os médicos do sistema
            this.medicosDisponiveis = new java.util.ArrayList<>(medicoService.buscarTodos());
            // Remove os médicos já vinculados à clínica selecionada
            if (this.selectedClinica != null && this.selectedClinica.getId() != null) {
                // Garante que a clínica está atualizada do banco
                this.selectedClinica = clinicaService.consultar(this.selectedClinica.getId());
                Set<Medico> medicosVinculados = this.selectedClinica.getMedicos();
                this.medicosDisponiveis.removeIf(medico -> medicosVinculados.stream().anyMatch(m -> m.getId().equals(medico.getId())));
            }
            // Atualiza a lista exibida
            this.medicosDisponiveisExibidos = this.medicosDisponiveis;
            // Se não houver médicos disponíveis, limpa seleção
            if (this.medicosDisponiveisExibidos == null || this.medicosDisponiveisExibidos.isEmpty()) {
                this.selectedMedicos = null;
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao preparar médicos disponíveis: " + e.getMessage()));
        }
    }
}