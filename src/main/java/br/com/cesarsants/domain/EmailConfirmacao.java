package br.com.cesarsants.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "email_confirmacao")
public class EmailConfirmacao implements Serializable, Persistente {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String codigo;
    
    @Column(nullable = false)
    private LocalDateTime dataExpiracao;
    
    @Column(nullable = false)
    private boolean confirmado = false;
    
    @Column(nullable = false)
    private LocalDateTime dataCriacao;
    
    // Construtor padrão
    public EmailConfirmacao() {
        this.dataCriacao = LocalDateTime.now();
    }
    
    // Construtor com parâmetros
    public EmailConfirmacao(String email, String codigo) {
        this();
        this.email = email;
        this.codigo = codigo;
        this.dataExpiracao = LocalDateTime.now().plusMinutes(15); // Expira em 15 minutos
    }
    
    // Getters e Setters
    @Override
    public Long getId() {
        return id;
    }
    
    @Override
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getCodigo() {
        return codigo;
    }
    
    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
    
    public LocalDateTime getDataExpiracao() {
        return dataExpiracao;
    }
    
    public void setDataExpiracao(LocalDateTime dataExpiracao) {
        this.dataExpiracao = dataExpiracao;
    }
    
    public boolean isConfirmado() {
        return confirmado;
    }
    
    public void setConfirmado(boolean confirmado) {
        this.confirmado = confirmado;
    }
    
    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }
    
    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
    
    // Método para verificar se o código expirou
    public boolean isExpirado() {
        boolean expirado = LocalDateTime.now().isAfter(this.dataExpiracao);
        System.out.println("=== VERIFICANDO EXPIRAÇÃO ===");
        System.out.println("Data atual: " + LocalDateTime.now());
        System.out.println("Data expiração: " + this.dataExpiracao);
        System.out.println("Expirado: " + expirado);
        return expirado;
    }
    
    // Método para verificar se pode ser confirmado
    public boolean podeSerConfirmado() {
        boolean podeConfirmar = !this.confirmado && !this.isExpirado();
        System.out.println("=== VERIFICANDO SE PODE CONFIRMAR ===");
        System.out.println("Confirmado: " + this.confirmado);
        System.out.println("Expirado: " + this.isExpirado());
        System.out.println("Pode confirmar: " + podeConfirmar);
        return podeConfirmar;
    }
} 