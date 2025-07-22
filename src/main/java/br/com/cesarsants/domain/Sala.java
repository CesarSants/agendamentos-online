package br.com.cesarsants.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "TB_SALA")
public class Sala implements Persistente, Comparable<Sala> {
    
    @Override
    public int compareTo(Sala other) {
        if (this.ordem == null || other.ordem == null) {
            return 0;
        }
        int ordemCompare = this.ordem.compareTo(other.ordem);
        if (ordemCompare != 0) {
            return ordemCompare;
        }
        // If ordem is the same, use ID to ensure consistent ordering
        if (this.id == null || other.id == null) {
            return 0;
        }
        return this.id.compareTo(other.id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Sala other = (Sala) obj;
        if (id == null) {
            if (other.id != null) return false;
        } else if (!id.equals(other.id)) return false;
        if (nome == null) {
            if (other.nome != null) return false;
        } else if (!nome.equals(other.nome)) return false;
        if (ordem == null) {
            if (other.ordem != null) return false;
        } else if (!ordem.equals(other.ordem)) return false;
        if (clinica == null) {
            if (other.clinica != null) return false;
        } else if (!clinica.getId().equals(other.clinica.getId())) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((nome == null) ? 0 : nome.hashCode());
        result = prime * result + ((ordem == null) ? 0 : ordem.hashCode());
        result = prime * result + ((clinica == null) ? 0 : clinica.getId().hashCode());
        return result;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sala_seq")
    @SequenceGenerator(name = "sala_seq", sequenceName = "sq_sala", initialValue = 1, allocationSize = 1)
    private Long id;

    @Column(name = "NOME", nullable = false)
    private String nome;

    @Column(name = "ORDEM", nullable = false)
    private Integer ordem;

    @ManyToOne
    @JoinColumn(name = "clinica_id", nullable = false)
    private Clinica clinica;

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

    public Integer getOrdem() {
        return ordem;
    }

    public void setOrdem(Integer ordem) {
        this.ordem = ordem;
    }

    public Clinica getClinica() {
        return clinica;
    }

    public void setClinica(Clinica clinica) {
        this.clinica = clinica;
    }
}
