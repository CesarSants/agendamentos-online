package br.com.cesarsants.domain;

import java.util.HashSet;
import java.util.Set;

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

@Entity
@Table(name = "TB_PACIENTE")
@NamedQuery(name = "Paciente.findByNome", query = "SELECT p FROM Paciente p WHERE p.nome LIKE :nome") 
public class Paciente implements Persistente {
    
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="paciente_seq")
    @SequenceGenerator(name="paciente_seq", sequenceName="sq_paciente", initialValue = 1, allocationSize = 1)
    private Long id;
    
    @Column(name = "NOME", nullable = false, length = 50)
    private String nome;
    
    @Column(name = "CPF", nullable = false)
    private Long cpf;
    
    @Column(name = "TELEFONE", nullable = false)
    private Long telefone;
    
    @Column(name = "ENDERECO", nullable = false, length = 100)
    private String endereco;
    
    @Column(name = "NUMERO_CONVENIO", nullable = false, length = 20)
    private String numeroConvenio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_USUARIO", nullable = false)
    private Usuario usuario;

    @OneToMany(mappedBy = "paciente", fetch = FetchType.LAZY)
    private Set<Agenda> agendamentos = new HashSet<>();

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

    public Long getTelefone() {
        return telefone;
    }

    public void setTelefone(Long telefone) {
        this.telefone = telefone;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public String getNumeroConvenio() {
        return numeroConvenio;
    }

    public void setNumeroConvenio(String numeroConvenio) {
        this.numeroConvenio = numeroConvenio;
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