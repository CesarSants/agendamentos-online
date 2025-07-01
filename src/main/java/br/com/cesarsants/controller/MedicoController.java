package br.com.cesarsants.controller;

import java.io.Serializable;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpSession;

import br.com.cesarsants.domain.Medico;
import br.com.cesarsants.domain.Usuario;
import br.com.cesarsants.exceptions.DAOException;
import br.com.cesarsants.service.IMedicoService;

/**
 * @author cesarsants
 *
 */

@Named
@ViewScoped
public class MedicoController implements Serializable {

    private static final long serialVersionUID = 3L;
    
    private Medico medico;
    private Collection<Medico> medicos;
    private Collection<Medico> medicosExibidos;
    
    @Inject
    private IMedicoService medicoService;
    
    private Boolean isUpdate;
    private String cpfMask;

    // Search properties
    private String searchText;
    private String searchType = "nome"; // default value
    
    @PostConstruct
    public void init() {
        try {
            this.isUpdate = false;
            this.medico = new Medico();
            this.medicos = medicoService.buscarTodos();
            this.medicosExibidos = this.medicos;
        } catch(Exception e) {
            FacesContext.getCurrentInstance().addMessage("msgs", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage()));
        }
    }
    
    // Método utilitário para formatar CRM com máscara
    public String formatarCrm(Long crm) {
        if (crm == null) return "";
        String crmStr = String.format("%06d", crm);
        return crmStr.substring(0, 2) + "." +
               crmStr.substring(2, 6);
    }

    // Método utilitário para formatar CPF com máscara
    public String formatarCpf(Long cpf) {
        if (cpf == null) return "";
        String cpfStr = String.format("%011d", cpf);
        return cpfStr.substring(0, 3) + "." +
               cpfStr.substring(3, 6) + "." +
               cpfStr.substring(6, 9) + "-" +
               cpfStr.substring(9, 11);
    }

    public void cancel() {
        try {
            this.isUpdate = false;
            this.medico = new Medico();
            this.cpfMask = null;
        } catch(Exception e) {
            FacesContext.getCurrentInstance().addMessage("msgs", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage()));
        }
    }
    
    public void edit(Medico medico) {
        try {
            this.isUpdate = true;
            // Clona o objeto para edição
            this.medico = new Medico();
            this.medico.setId(medico.getId());
            this.medico.setNome(medico.getNome());
            this.medico.setCpf(medico.getCpf());
            this.medico.setEndereco(medico.getEndereco());
            this.medico.setTempoConsulta(medico.getTempoConsulta());
            // Preenche o campo cpfMask com o valor puro do CPF (sem máscara)
            this.cpfMask = medico.getCpf() != null ? medico.getCpf().toString() : "";
        } catch(Exception e) {
            FacesContext.getCurrentInstance().addMessage("msgs", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage()));
        }
    }
      public void delete(Medico medico) {
        try {
            medicoService.excluir(medico);
            this.medicos.remove(medico);
            this.medicosExibidos.remove(medico); // Atualiza também a lista filtrada
            FacesContext.getCurrentInstance().addMessage("msgs", 
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Médico excluído com sucesso"));
        } catch(DAOException e) {
            FacesContext.getCurrentInstance().addMessage("msgs", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Não foi possível excluir", e.getMessage()));
        } catch(Exception e) {
            e.printStackTrace(); // Log do erro para debug
            FacesContext.getCurrentInstance().addMessage("msgs", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro inesperado", "Ocorreu um erro ao tentar excluir o médico"));
        }
    }
    
    public void add() {
        try {
            removerCaracteresInvalidos();
            
            // Associa o usuário logado ao médico
            Usuario usuarioLogado = getUsuarioLogado();
            if (usuarioLogado == null) {
                throw new Exception("Usuário não autenticado");
            }
            this.medico.setUsuario(usuarioLogado);
            
            // Validação de nome único - apenas verifica se existe, não reutiliza o objeto
            Medico medicoNomeDb = medicoService.buscarPorNomeExato(this.getMedico().getNome(), usuarioLogado);
            if (medicoNomeDb != null && (this.medico.getId() == null || !medicoNomeDb.getId().equals(this.medico.getId()))) {
                throw new Exception("Nome de médico já cadastrado");
            }
            
            // Validação de CPF único - apenas verifica se existe, não reutiliza o objeto
            Medico medicoDb = medicoService.buscarPorCPF(this.getMedico().getCpf(), usuarioLogado);
            if (medicoDb != null && (this.medico.getId() == null || !medicoDb.getId().equals(this.medico.getId()))) {
                throw new Exception("CPF já cadastrado");
            }
            
            if (this.isUpdate) {
                medicoService.alterar(this.medico);
            } else {
                medicoService.cadastrar(this.medico);
            }
            // Update both lists, clear form, and reset update flag
            this.medicos = medicoService.buscarTodos();
            this.medicosExibidos = this.medicos; // Update the filtered list too
            this.searchText = ""; // Clear search
            this.medico = new Medico();
            this.isUpdate = false;
            FacesContext.getCurrentInstance().addMessage("msgs", 
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Médico salvo com sucesso"));
        } catch(Exception e) {
            FacesContext.getCurrentInstance().addMessage("msgs", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage()));
        }
    }
    
    public void removerCaracteresInvalidos() {
        if (this.cpfMask != null && !this.cpfMask.isEmpty()) {
            this.medico.setCpf(Long.valueOf(this.cpfMask.replaceAll("[^\\d]", "")));
        }
    }
    
    public void findByNome() {
        try {
            if (this.medico != null && this.medico.getNome() != null && !this.medico.getNome().isEmpty()) {
                this.medicos = medicoService.filtrarMedicos(this.medico.getNome());
            } else {
                this.medicos = medicoService.buscarTodos();
            }
        } catch(Exception e) {
            FacesContext.getCurrentInstance().addMessage("msgs", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage()));
        }
    }

    public void filterMedicos() {
        try {
            if (searchText == null || searchText.trim().isEmpty()) {
                this.medicosExibidos = this.medicos;
                return;
            }

            this.medicosExibidos = this.medicos.stream()
                .filter(m -> {
                    switch (searchType) {
                        case "nome":
                            return m.getNome().toLowerCase().contains(searchText.toLowerCase());
                        case "crm":
                            if (m.getCpf() == null) return false;
                            String crmSearch = searchText.replaceAll("[^0-9]", "");
                            String crmValue = String.valueOf(m.getCpf());
                            return crmValue.contains(crmSearch);
                        case "especialidade":
                            return m.getEndereco() != null && 
                                   m.getEndereco().toLowerCase().contains(searchText.toLowerCase());
                        default:
                            return false;
                    }
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao filtrar médicos: " + e.getMessage()));
        }
    }

    public void clearSearch() {
        try {
            this.searchText = "";
            this.medicosExibidos = this.medicos;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao limpar busca: " + e.getMessage()));
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

    public Medico getMedico() {
        return medico;
    }

    public void setMedico(Medico medico) {
        this.medico = medico;
    }

    public Collection<Medico> getMedicos() {
        return medicos;
    }

    public void setMedicos(Collection<Medico> medicos) {
        this.medicos = medicos;
    }

    public Boolean getIsUpdate() {
        return isUpdate;
    }

    public void setIsUpdate(Boolean isUpdate) {
        this.isUpdate = isUpdate;
    }

    public String getCpfMask() {
        return cpfMask;
    }

    public void setCpfMask(String cpfMask) {
        this.cpfMask = cpfMask;
    }

    // Getters and setters for search properties
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

    public Collection<Medico> getMedicosExibidos() {
        if (medicosExibidos == null) {
            medicosExibidos = medicos;
        }
        return medicosExibidos;
    }

    public void setMedicosExibidos(Collection<Medico> medicosExibidos) {
        this.medicosExibidos = medicosExibidos;
    }
}