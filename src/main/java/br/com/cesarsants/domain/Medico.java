package br.com.cesarsants.domain;

import java.util.HashSet;
import java.util.Set;

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

@Entity
@Table(name = "TB_MEDICO")
@NamedQuery(name = "Medico.findByNome", query = "SELECT m FROM Medico m WHERE m.nome LIKE :nome")
public class Medico implements Persistente {

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="medico_seq")
    @SequenceGenerator(name="medico_seq", sequenceName="sq_medico", initialValue = 1, allocationSize = 1)
    private Long id;

    @Column(name = "NOME", nullable = false)
    private String nome;

    @Column(name = "CPF", nullable = false)
    private Long cpf;

    @Column(name = "ENDERECO")
    private String endereco;

    @Column(name = "TEMPO_CONSULTA")
    private Integer tempoConsulta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_USUARIO", nullable = false)
    private Usuario usuario;

    @OneToMany(mappedBy = "medico", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MedicoClinica> clinicasMedico = new HashSet<>();

    @OneToMany(mappedBy = "medico", fetch = FetchType.LAZY)
    private Set<Agenda> agendamentos = new HashSet<>();

    @Transient
    public Set<Clinica> getClinicas() {
        Set<Clinica> clinicas = new HashSet<>();
        for (MedicoClinica mc : clinicasMedico) {
            clinicas.add(mc.getClinica());
        }
        return clinicas;
    }

    // Helper methods for managing relationships
    public void addClinica(Clinica clinica) {
        MedicoClinica medicoClinica = new MedicoClinica();
        medicoClinica.setMedico(this);
        medicoClinica.setClinica(clinica);
        clinicasMedico.add(medicoClinica);
    }

    public void removeClinica(Clinica clinica) {
        clinicasMedico.removeIf(mc -> mc.getClinica().equals(clinica));
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

    public Long getCpf() {
        return cpf;
    }

    public void setCpf(Long cpf) {
        this.cpf = cpf;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public Integer getTempoConsulta() {
        return tempoConsulta;
    }

    public void setTempoConsulta(Integer tempoConsulta) {
        this.tempoConsulta = tempoConsulta;
    }

    public Set<MedicoClinica> getClinicasMedico() {
        return clinicasMedico;
    }

    public void setClinicasMedico(Set<MedicoClinica> clinicasMedico) {
        this.clinicasMedico = clinicasMedico;
    }

    public Set<Agenda> getAgendamentos() {
        return agendamentos;
    }

    public void setAgendamentos(Set<Agenda> agendamentos) {
        this.agendamentos = agendamentos;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}