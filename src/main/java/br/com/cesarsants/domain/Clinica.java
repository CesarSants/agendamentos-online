package br.com.cesarsants.domain;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * @author cesarsants
 *
 */

@Entity
@Table(name = "TB_CLINICA")
@NamedQuery(name = "Clinica.findByNome", query = "SELECT c FROM Clinica c WHERE c.nome LIKE :nome")
public class Clinica implements Persistente {

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="clinica_seq")
    @SequenceGenerator(name="clinica_seq", sequenceName="sq_clinica", initialValue = 1, allocationSize = 1)
    private Long id;

    @Column(name = "NOME", nullable = false)
    private String nome;

    @Column(name = "CNPJ", nullable = false)
    private Long cnpj;

    @Column(name = "ENDERECO")
    private String endereco;

    @Column(name = "NUMERO_TOTAL_SALAS")
    private Integer numeroTotalSalas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_USUARIO", nullable = false)
    private Usuario usuario;

    @OneToMany(mappedBy = "clinica", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Sala> salas = new TreeSet<>((s1, s2) -> {
        if (s1.getOrdem() == null || s2.getOrdem() == null) {
            return 0;
        }
        return s1.getOrdem().compareTo(s2.getOrdem());
    });

    @OneToMany(mappedBy = "clinica", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MedicoClinica> medicosClinica = new HashSet<>();

    @OneToMany(mappedBy = "clinica", fetch = FetchType.LAZY)
    private Set<Agenda> agendamentos = new HashSet<>();

    @Column(name = "HORARIO_ABERTURA", nullable = false)
    private LocalTime horarioAbertura;

    @Column(name = "HORARIO_FECHAMENTO", nullable = false)
    private LocalTime horarioFechamento;

    @Transient
    public Set<Medico> getMedicos() {
        Set<Medico> medicos = new HashSet<>();
        for (MedicoClinica mc : medicosClinica) {
            medicos.add(mc.getMedico());
        }
        return medicos;
    }

    // Helper methods for managing relationships
    public void addMedico(Medico medico) {
        MedicoClinica medicoClinica = new MedicoClinica();
        medicoClinica.setMedico(medico);
        medicoClinica.setClinica(this);
        medicosClinica.add(medicoClinica);
    }

    public void removeMedico(Medico medico) {
        medicosClinica.removeIf(mc -> mc.getMedico().equals(medico));
    }

    public void removerMedicoClinica(MedicoClinica medicoClinica) {
        medicosClinica.remove(medicoClinica);
        medicoClinica.setClinica(null);
        medicoClinica.setMedico(null);
    }

    public void adicionarMedicoClinica(MedicoClinica medicoClinica) {
        medicosClinica.add(medicoClinica);
        medicoClinica.setClinica(this);
    }

    public void initializeSalas() {
        salas.clear();
        if (numeroTotalSalas != null) {
            for (int i = 1; i <= numeroTotalSalas; i++) {
                Sala sala = new Sala();
                sala.setNome("Sala " + i);
                sala.setOrdem(i);
                sala.setClinica(this);
                salas.add(sala);
            }
        }
    }

    public Set<Sala> getSalas() {
        return salas;
    }

    public void setSalas(Set<Sala> salas) {
        this.salas = salas;
    }

    public void setNumeroTotalSalas(Integer newNumeroTotalSalas) {
        if (this.salas == null) {
            this.salas = new TreeSet<>((s1, s2) -> {
                if (s1.getOrdem() == null || s2.getOrdem() == null) {
                    return 0;
                }
                return s1.getOrdem().compareTo(s2.getOrdem());
            });
        }

        this.numeroTotalSalas = newNumeroTotalSalas;

        if (newNumeroTotalSalas == null || newNumeroTotalSalas <= 0) {
            this.salas.clear();
            return;
        }

        // Convert to list for easier manipulation and preserve ordem-based ordering
        List<Sala> currentSalas = new ArrayList<>(this.salas);
        currentSalas.sort(Comparator.comparing(Sala::getOrdem, Comparator.nullsLast(Integer::compareTo)));

        // Handle room count decrease
        if (currentSalas.size() > newNumeroTotalSalas) {
            // Keep first N rooms (lowest ordem values), remove the rest
            this.salas.clear();
            for (int i = 0; i < newNumeroTotalSalas; i++) {
                this.salas.add(currentSalas.get(i));
            }
        } 
        // Handle room count increase
        else if (currentSalas.size() < newNumeroTotalSalas) {
            // Determine next ordem value
            int nextOrder = currentSalas.isEmpty() ? 1 : 
                          currentSalas.get(currentSalas.size() - 1).getOrdem() + 1;

            // First ensure all existing rooms are preserved in the set
            this.salas.clear();
            this.salas.addAll(currentSalas);

            // Add new rooms with sequential ordem numbers
            for (int i = currentSalas.size() + 1; i <= newNumeroTotalSalas; i++) {
                Sala sala = new Sala();
                sala.setNome("Sala " + nextOrder);
                sala.setOrdem(nextOrder);
                sala.setClinica(this);
                this.salas.add(sala);
                nextOrder++;
            }
        }
        // If sizes match, no action needed - existing rooms are preserved
    }

    public Integer getNumeroTotalSalas() {
        return numeroTotalSalas;
    }

    // Getters and Setters
    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Long getCnpj() {
        return cnpj;
    }

    public void setCnpj(Long cnpj) {
        this.cnpj = cnpj;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public Set<MedicoClinica> getMedicosClinica() {
        return medicosClinica;
    }

    public void setMedicosClinica(Set<MedicoClinica> medicosClinica) {
        this.medicosClinica = medicosClinica;
    }

    public Set<Agenda> getAgendamentos() {
        return agendamentos;
    }

    public void setAgendamentos(Set<Agenda> agendamentos) {
        this.agendamentos = agendamentos;
    }

    public LocalTime getHorarioAbertura() {
        return horarioAbertura;
    }

    public void setHorarioAbertura(LocalTime horarioAbertura) {
        this.horarioAbertura = horarioAbertura;
    }

    public LocalTime getHorarioFechamento() {
        return horarioFechamento;
    }

    public void setHorarioFechamento(LocalTime horarioFechamento) {
        this.horarioFechamento = horarioFechamento;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}