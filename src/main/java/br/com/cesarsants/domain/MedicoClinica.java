package br.com.cesarsants.domain;

import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * @author cesarsants
 *
 */

@Entity
@Table(name = "TB_MEDICO_CLINICA")
public class MedicoClinica implements Persistente {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "medico_clinica_seq")
    @SequenceGenerator(name = "medico_clinica_seq", sequenceName = "sq_medico_clinica", initialValue = 1, allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "medico_id", nullable = false, foreignKey = @ForeignKey(name = "FK_MEDICO_CLINICA_MEDICO"))
    private Medico medico;

    @ManyToOne
    @JoinColumn(name = "clinica_id", nullable = false, foreignKey = @ForeignKey(name = "FK_MEDICO_CLINICA_CLINICA"))
    private Clinica clinica;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Medico getMedico() {
        return medico;
    }

    public void setMedico(Medico medico) {
        this.medico = medico;
    }

    public Clinica getClinica() {
        return clinica;
    }

    public void setClinica(Clinica clinica) {
        this.clinica = clinica;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MedicoClinica that = (MedicoClinica) obj;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
