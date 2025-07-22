package br.com.cesarsants.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "TB_AGENDA")
public class Agenda implements Persistente {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "agenda_seq")
    @SequenceGenerator(name = "agenda_seq", sequenceName = "sq_agenda", initialValue = 1, allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "clinica_id", nullable = false)
    private Clinica clinica;

    @ManyToOne
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;

    @ManyToOne
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_USUARIO", nullable = false)
    private Usuario usuario;

    @Column(name = "DATA_HORA", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "DATA_HORA_FIM", nullable = false)
    private LocalDateTime dataHoraFim;

    @Column(name = "NUMERO_SALA", nullable = false)
    private Integer numeroSala;

    @Column(name = "OBSERVACOES")
    private String observacoes;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false)
    private Status status = Status.AGENDADO;

    @ManyToOne
    @JoinColumn(name = "id_sala")
    private Sala sala;

    public enum Status {
        AGENDADO("Agendado", "info", "status-scheduled", true),
        CONCLUIDO("Concluído", "success", "status-completed", false),
        CANCELADO("Cancelado", "danger", "status-cancelled", false);

        private String descricao;
        private String severity;
        private String cssClass;
        private boolean cancelavel;

        Status(String descricao, String severity, String cssClass, boolean cancelavel) {
            this.descricao = descricao;
            this.severity = severity;
            this.cssClass = cssClass;
            this.cancelavel = cancelavel;
        }

        public String getDescricao() {
            return descricao;
        }

        public String getSeverity() {
            return severity;
        }

        public String getCssClass() {
            return cssClass;
        }

        public boolean isCancelavel() {
            return cancelavel;
        }
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Clinica getClinica() {
        return clinica;
    }

    public void setClinica(Clinica clinica) {
        this.clinica = clinica;
    }

    public Medico getMedico() {
        return medico;
    }

    public void setMedico(Medico medico) {
        this.medico = medico;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public LocalDateTime getDataHoraFim() {
        return dataHoraFim;
    }

    public void setDataHoraFim(LocalDateTime dataHoraFim) {
        this.dataHoraFim = dataHoraFim;
    }

    public Integer getNumeroSala() {
        return numeroSala;
    }

    public void setNumeroSala(Integer numeroSala) {
        this.numeroSala = numeroSala;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Sala getSala() {
        return sala;
    }

    public void setSala(Sala sala) {
        this.sala = sala;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}