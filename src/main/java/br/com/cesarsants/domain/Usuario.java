package br.com.cesarsants.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * @author cesarsants
 *
 */

@Entity
@Table(name = "TB_USUARIO")
public class Usuario implements Persistente {
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "usuario_seq")
	@SequenceGenerator(name = "usuario_seq", sequenceName = "sq_usuario", initialValue = 1, allocationSize = 1)
	private Long id;
	
	@Column(name = "NOME", nullable = false, length = 100)
	private String nome;
	
	@Column(name = "EMAIL", nullable = false, unique = true, length = 100)
	private String email;
	
	@Column(name = "SENHA", nullable = false, length = 100)
	private String senha;
	
	@Column(name = "ATUALIZACAO_AUTOMATICA_ATIVA", nullable = false)
	private boolean atualizacaoAutomaticaAtiva = true;
	
	public Usuario() {
	}
	
	public Usuario(String nome, String email, String senha) {
		this.nome = nome;
		this.email = email;
		this.senha = senha;
	}

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

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getSenha() {
		return senha;
	}

	public void setSenha(String senha) {
		this.senha = senha;
	}

	public boolean isAtualizacaoAutomaticaAtiva() {
		return atualizacaoAutomaticaAtiva;
	}

	public void setAtualizacaoAutomaticaAtiva(boolean atualizacaoAutomaticaAtiva) {
		this.atualizacaoAutomaticaAtiva = atualizacaoAutomaticaAtiva;
	}
} 