package br.com.cesarsants.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.servlet.http.HttpSession;

import br.com.cesarsants.dao.IClinicaDAO;
import br.com.cesarsants.domain.Clinica;
import br.com.cesarsants.domain.Medico;
import br.com.cesarsants.domain.MedicoClinica;
import br.com.cesarsants.domain.Sala;
import br.com.cesarsants.domain.Usuario;
import br.com.cesarsants.exceptions.DAOException;
import br.com.cesarsants.exceptions.MaisDeUmRegistroException;
import br.com.cesarsants.exceptions.TableException;
import br.com.cesarsants.exceptions.TipoChaveNaoEncontradaException;
import br.com.cesarsants.services.generic.GenericService;

/**
 * @author cesarsants
 *
 */

@ApplicationScoped
public class ClinicaService extends GenericService<Clinica, Long> implements IClinicaService {
    
    private final IClinicaDAO clinicaDAO;
    private final IMedicoService medicoService;
    private final IMedicoClinicaService medicoClinicaService;
      
    @Inject
    public ClinicaService(IClinicaDAO clinicaDAO, IMedicoService medicoService, IMedicoClinicaService medicoClinicaService) {
        super(clinicaDAO);
        this.clinicaDAO = clinicaDAO;
        this.medicoService = medicoService;
        this.medicoClinicaService = medicoClinicaService;
    }

    public Clinica buscarPorCNPJ(Long cnpj, Usuario usuario) throws DAOException {
        return this.clinicaDAO.buscarPorCNPJ(cnpj, usuario);
    }

    @Override
    public List<Clinica> filtrarClinicas(String query) {
        Usuario usuarioLogado = getUsuarioLogado();
        return clinicaDAO.filtrarClinicas(query, usuarioLogado);
    }
    
    @Override
    public List<Clinica> buscarTodos() {
        Usuario usuarioLogado = getUsuarioLogado();
        return clinicaDAO.buscarTodos(usuarioLogado);
    }
    
    private Usuario getUsuarioLogado() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(false);
            if (session != null) {
                return (Usuario) session.getAttribute("usuarioLogado");
            }
        }
        return null;
    }

    @Override
    public boolean verificarHorarioFuncionamento(Long clinicaId, LocalDateTime horario) {
        try {
            Clinica clinica = super.consultar(clinicaId);
            LocalTime horaConsulta = horario.toLocalTime();
            
            // Caso 1: Horário de abertura igual ao horário de fechamento (24h)
            if (clinica.getHorarioAbertura().equals(clinica.getHorarioFechamento())) {
                return true;
            }
            
            // Caso 2: Horário de abertura menor que horário de fechamento (Ex: 09:00 - 18:00)
            if (clinica.getHorarioAbertura().isBefore(clinica.getHorarioFechamento())) {
                return !horaConsulta.isBefore(clinica.getHorarioAbertura()) && 
                       !horaConsulta.isAfter(clinica.getHorarioFechamento());
            }
            
            // Caso 3: Horário de abertura maior que horário de fechamento (Ex: 20:00 - 10:00)
            return !horaConsulta.isBefore(clinica.getHorarioAbertura()) || 
                   !horaConsulta.isAfter(clinica.getHorarioFechamento());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean verificarDisponibilidadeSala(Long clinicaId, Integer numeroSala, 
            LocalDateTime horario, Integer duracaoConsulta) {
        Usuario usuarioLogado = getUsuarioLogado();
        return clinicaDAO.isSalaDisponivel(clinicaId, numeroSala, horario, duracaoConsulta, usuarioLogado);
    }

    @Override
    public void adicionarMedico(Long clinicaId, Long medicoId) throws Exception {
        try {
            // Busca a clínica e o médico
            Clinica clinica = super.consultar(clinicaId);
            Medico medico = medicoService.consultar(medicoId);
            
            if (clinica == null || medico == null) {
                throw new DAOException("Clínica ou Médico não encontrados", null);
            }

            // Verifica se o vínculo já existe
            List<MedicoClinica> vinculos = medicoClinicaService.buscarPorClinica(clinicaId);
            boolean vinculoExistente = vinculos.stream()
                .anyMatch(mc -> mc.getMedico().getId().equals(medicoId));

            if (vinculoExistente) {
                throw new DAOException("Este médico já está vinculado a esta clínica", null);
            }

            // Se não existe, cria o novo vínculo
            MedicoClinica medicoClinica = new MedicoClinica();
            medicoClinica.setClinica(clinica);
            medicoClinica.setMedico(medico);
            medicoClinicaService.cadastrar(medicoClinica);

        } catch (Exception e) {
            throw new DAOException("Erro ao vincular médico à clínica: " + e.getMessage(), e);
        }
    }

    @Override
    public void removerMedico(Long clinicaId, Long medicoId) throws Exception {
        try {
            // Busca a clínica
            Clinica clinica = this.consultar(clinicaId);
            if (clinica == null) {
                throw new DAOException("Clínica não encontrada", null);
            }

            // Busca os vínculos existentes
            List<MedicoClinica> vinculos = medicoClinicaService.buscarPorClinica(clinicaId);
            
            // Encontra o vínculo específico
            MedicoClinica vinculoParaRemover = vinculos.stream()
                .filter(mc -> mc.getMedico().getId().equals(medicoId))
                .findFirst()
                .orElseThrow(() -> new DAOException("Vínculo não encontrado", null));
            
            // Remove o vínculo da coleção da clínica
            clinica.getMedicosClinica().remove(vinculoParaRemover);
            
            // Atualiza a clínica
            this.alterar(clinica);
            
            // Remove o vínculo
            medicoClinicaService.excluir(vinculoParaRemover);
            
        } catch (Exception e) {
            throw new DAOException("Erro ao desvincular médico da clínica: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Clinica> buscarPorMedico(Long medicoId) {
        Usuario usuarioLogado = getUsuarioLogado();
        return clinicaDAO.buscarPorMedico(medicoId, usuarioLogado);
    }

    @Override
    public Clinica alterar(Clinica clinica) throws DAOException, TipoChaveNaoEncontradaException {
        try {
            // Se houver mudança no número de salas, usa o método específico
            if (clinica.getId() != null) {
                Clinica clinicaAtual = super.consultar(clinica.getId());
                if (clinicaAtual != null && 
                    !clinicaAtual.getNumeroTotalSalas().equals(clinica.getNumeroTotalSalas())) {
                    
                    // Atualiza outros campos da clínica
                    clinicaAtual.setNome(clinica.getNome());
                    clinicaAtual.setCnpj(clinica.getCnpj());
                    clinicaAtual.setEndereco(clinica.getEndereco());
                    clinicaAtual.setHorarioAbertura(clinica.getHorarioAbertura());
                    clinicaAtual.setHorarioFechamento(clinica.getHorarioFechamento());

                    // Usa o método específico para atualizar salas preservando dados
                    return atualizarNumeroSalas(clinica.getId(), clinica.getNumeroTotalSalas());
                }
            }

            // Se não houve mudança no número de salas, procede com a atualização normal
            return super.alterar(clinica);
        } catch (MaisDeUmRegistroException | TableException e) {
            throw new DAOException("Erro ao alterar clínica: " + e.getMessage(), e);
        }
    }

    /**
     * Atualiza o número total de salas de uma clínica preservando os dados existentes
     */
    @Override
    public Clinica atualizarNumeroSalas(Long clinicaId, Integer novoNumeroTotalSalas) 
            throws DAOException, TipoChaveNaoEncontradaException {
        try {
            // Busca a clínica atual do banco de dados
            Clinica clinica = super.consultar(clinicaId);
            if (clinica == null) {
                throw new DAOException("Clínica não encontrada", null);
            }

            // Se não houver salas, inicializa a coleção
            if (clinica.getSalas() == null) {
                clinica.setSalas(new TreeSet<>((s1, s2) -> {
                    if (s1.getOrdem() == null || s2.getOrdem() == null) {
                        return 0;
                    }
                    return s1.getOrdem().compareTo(s2.getOrdem());
                }));
            }

            // Obtém a lista atual de salas ordenada por ordem
            List<Sala> salasAtuais = new ArrayList<>(clinica.getSalas());
            salasAtuais.sort(Comparator.comparing(Sala::getOrdem, Comparator.nullsLast(Integer::compareTo)));
            
            // Caso de diminuição do número de salas
            if (salasAtuais.size() > novoNumeroTotalSalas) {
                // Remove as salas excedentes (mantém as N primeiras)
                List<Sala> salasPreservadas = salasAtuais.subList(0, novoNumeroTotalSalas);
                clinica.getSalas().clear();
                clinica.getSalas().addAll(salasPreservadas);
            }
            // Caso de aumento do número de salas
            else if (salasAtuais.size() < novoNumeroTotalSalas) {
                // Determina o próximo número de ordem
                int nextOrder = salasAtuais.isEmpty() ? 1 : 
                            salasAtuais.get(salasAtuais.size() - 1).getOrdem() + 1;

                // Adiciona novas salas mantendo a sequência
                for (int i = salasAtuais.size() + 1; i <= novoNumeroTotalSalas; i++) {
                    Sala novaSala = new Sala();
                    novaSala.setNome("Sala " + nextOrder);
                    novaSala.setOrdem(nextOrder);
                    novaSala.setClinica(clinica);
                    clinica.getSalas().add(novaSala);
                    nextOrder++;
                }
            }
            // Se o número for igual, não precisa fazer nada

            // Atualiza o número total de salas
            clinica.setNumeroTotalSalas(novoNumeroTotalSalas);

            // Persiste as alterações e retorna a clínica atualizada
            return super.alterar(clinica);
        } catch (MaisDeUmRegistroException | TableException e) {
            throw new DAOException("Erro ao atualizar número de salas: " + e.getMessage(), e);
        }
    }
}