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

import br.com.cesarsants.domain.Paciente;
import br.com.cesarsants.service.IPacienteService;

/**
 * @author cesarsants
 *
 */

@Named
@ViewScoped
public class PacienteController implements Serializable {

    private static final long serialVersionUID = 4L;
    
    private Paciente paciente;
    private Collection<Paciente> pacientes;
    private Collection<Paciente> pacientesExibidos;
    
    @Inject
    private IPacienteService pacienteService;
    
    private Boolean isUpdate;
    private String cpfMask;
    private String telMask;

    // Search properties
    private String searchText;
    private String searchType = "nome"; // default value
    
    @PostConstruct
    public void init() {
        try {
            this.isUpdate = false;
            this.paciente = new Paciente();
            this.pacientes = pacienteService.buscarTodos();
            this.pacientesExibidos = this.pacientes;
        } catch(Exception e) {
            FacesContext.getCurrentInstance().addMessage("msgs", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage()));
        }
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

    public void filterPacientes() {
        try {
            if (searchText == null || searchText.trim().isEmpty()) {
                this.pacientesExibidos = this.pacientes;
                return;
            }

            this.pacientesExibidos = this.pacientes.stream()
                .filter(p -> {
                    switch (searchType) {
                        case "nome":
                            return p.getNome().toLowerCase().contains(searchText.toLowerCase());                        case "cpf":
                            if (p.getCpf() == null) return false;
                            String cpfSearch = searchText.replaceAll("[^0-9]", "");
                            String cpfValue = String.valueOf(p.getCpf());
                            return cpfValue.contains(cpfSearch);
                        case "telefone":
                            if (p.getTelefone() == null) return false;
                            String telSearch = searchText.replaceAll("[^0-9]", "");
                            String telValue = String.valueOf(p.getTelefone());
                            return telValue.contains(telSearch);
                        case "endereco":
                            return p.getEndereco() != null && 
                                   p.getEndereco().toLowerCase().contains(searchText.toLowerCase());
                        default:
                            return false;
                    }
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao filtrar pacientes: " + e.getMessage()));
        }
    }

    public void clearSearch() {
        try {
            this.searchText = "";
            this.pacientesExibidos = this.pacientes;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao limpar busca: " + e.getMessage()));
        }
    }
    
    public void cancel() {
        try {
            this.isUpdate = false;
            this.paciente = new Paciente();
            this.cpfMask = null;
            this.telMask = null;
        } catch(Exception e) {
            FacesContext.getCurrentInstance().addMessage("msgs", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage()));
        }
    }
    
    public void edit(Paciente paciente) {
        try {
            this.isUpdate = true;
            this.paciente = new Paciente();
            this.paciente.setId(paciente.getId());
            this.paciente.setNome(paciente.getNome());
            this.paciente.setCpf(paciente.getCpf());
            this.paciente.setTelefone(paciente.getTelefone());
            this.paciente.setEndereco(paciente.getEndereco());
            this.paciente.setNumeroConvenio(paciente.getNumeroConvenio());
            this.cpfMask = paciente.getCpf() != null ? paciente.getCpf().toString() : "";
            this.telMask = paciente.getTelefone() != null ? paciente.getTelefone().toString() : "";
        } catch(Exception e) {
            FacesContext.getCurrentInstance().addMessage("msgs", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage()));
        }
    }
    
    public void delete(Paciente paciente) {
        try {
            pacienteService.excluir(paciente);
            this.pacientes.remove(paciente);
            FacesContext.getCurrentInstance().addMessage("msgs", 
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Paciente excluído com sucesso"));
        } catch(Exception e) {
            FacesContext.getCurrentInstance().addMessage("msgs", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage()));
        }
    }
    
    public void add() {
        try {
            removerCaracteresInvalidos();
            
            // Obtém o usuário logado da sessão
            FacesContext facesContext = FacesContext.getCurrentInstance();
            javax.servlet.http.HttpSession session = (javax.servlet.http.HttpSession) facesContext.getExternalContext().getSession(false);
            br.com.cesarsants.domain.Usuario usuarioLogado = (br.com.cesarsants.domain.Usuario) session.getAttribute("usuarioLogado");
            
            if (usuarioLogado == null) {
                throw new Exception("Usuário não está logado");
            }
            
            // Validação de nome único - apenas verifica se existe, não reutiliza o objeto
            Paciente pacienteNomeDb = pacienteService.buscarPorNomeExato(this.getPaciente().getNome(), usuarioLogado);
            if (pacienteNomeDb != null && (this.paciente.getId() == null || !pacienteNomeDb.getId().equals(this.paciente.getId()))) {
                throw new Exception("Nome de paciente já cadastrado");
            }
            
            // Validação de CPF único - apenas verifica se existe, não reutiliza o objeto
            Paciente pacienteDb = pacienteService.buscarPorCPF(this.getPaciente().getCpf(), usuarioLogado);
            if (pacienteDb != null && (this.paciente.getId() == null || !pacienteDb.getId().equals(this.paciente.getId()))) {
                throw new Exception("CPF já cadastrado");
            }
            
            // Associa o usuário logado ao paciente
            this.paciente.setUsuario(usuarioLogado);
            
            if (this.isUpdate) {
                pacienteService.alterar(this.paciente);
            } else {
                pacienteService.cadastrar(this.paciente);
            }
            
            // Update both lists, clear form, and reset update flag
            this.pacientes = pacienteService.buscarTodos();
            this.pacientesExibidos = this.pacientes; // Update the filtered list too
            this.searchText = ""; // Clear search
            this.paciente = new Paciente();
            this.cpfMask = null; // Clear masks
            this.telMask = null;
            this.isUpdate = false;
            
            FacesContext.getCurrentInstance().addMessage("msgs", 
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Paciente salvo com sucesso"));
        } catch(Exception e) {
            FacesContext.getCurrentInstance().addMessage("msgs", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage()));
        }
    }
    
    public void removerCaracteresInvalidos() {
        if (this.cpfMask != null && !this.cpfMask.isEmpty()) {
            this.paciente.setCpf(Long.valueOf(this.cpfMask.replaceAll("[^\\d]", "")));
        }
        if (this.telMask != null && !this.telMask.isEmpty()) {
            this.paciente.setTelefone(Long.valueOf(this.telMask.replaceAll("[^\\d]", "")));
        }
    }
    
    public void findByNome() {
        try {
            if (this.paciente != null && this.paciente.getNome() != null && !this.paciente.getNome().isEmpty()) {
                this.pacientes = pacienteService.filtrarPacientes(this.paciente.getNome());
            } else {
                this.pacientes = pacienteService.buscarTodos();
            }
        } catch(Exception e) {
            FacesContext.getCurrentInstance().addMessage("msgs", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage()));
        }
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
    }

    public Collection<Paciente> getPacientes() {
        return pacientes;
    }

    public void setPacientes(Collection<Paciente> pacientes) {
        this.pacientes = pacientes;
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

    public String getTelMask() {
        return telMask;
    }

    public void setTelMask(String telMask) {
        this.telMask = telMask;
    }

    // New getters and setters for search properties
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

    public Collection<Paciente> getPacientesExibidos() {
        if (pacientesExibidos == null) {
            pacientesExibidos = pacientes;
        }
        return pacientesExibidos;
    }

    public void setPacientesExibidos(Collection<Paciente> pacientesExibidos) {
        this.pacientesExibidos = pacientesExibidos;
    }
}