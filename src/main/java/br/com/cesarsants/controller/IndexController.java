/**
 * 
 */
package br.com.cesarsants.controller;

import java.io.Serializable;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

/**
 * @author cesarsants
 *
 */
@ManagedBean(name = "indexController")
@ViewScoped
public class IndexController implements Serializable {

	private static final long serialVersionUID = -784519597996507487L;

	public String redirectPaciente() {
		return "/paciente/list.xhtml";
	}
	
	public String redirectMedico() {
		return "/medico/list.xhtml";
	}
	
	public String redirectClinica() {
		return "/clinica/list.xhtml";
	}
	
	public String redirectAgenda() {
		return "/agenda/list.xhtml";
	}
}
