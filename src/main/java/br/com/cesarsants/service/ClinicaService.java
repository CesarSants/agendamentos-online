package br.com.cesarsants.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import javax.faces.context.FacesContext;
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

public class ClinicaService extends GenericService<Clinica, Long> implements IClinicaService {
    
    private final IClinicaDAO clinicaDAO;
    private IMedicoService medicoService;
    private final IMedicoClinicaService medicoClinicaService;
      
    public ClinicaService() {
        this(new br.com.cesarsants.dao.ClinicaDAO(), null, new br.com.cesarsants.service.MedicoClinicaService());
    }

    public ClinicaService(IClinicaDAO clinicaDAO, IMedicoService medicoService, IMedicoClinicaService medicoClinicaService) {
        super(clinicaDAO);
        this.clinicaDAO = clinicaDAO;
        this.medicoService = medicoService;
        this.medicoClinicaService = medicoClinicaService;
    }

    private IMedicoService getMedicoService() {
        if (medicoService == null) {
            medicoService = new br.com.cesarsants.service.MedicoService();
        }
        return medicoService;
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
        if (facesContext == null) {
            throw new IllegalStateException("getUsuarioLogado() chamado fora de contexto JSF! Não use este método em schedulers ou threads de background.");
        }
        HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(false);
        if (session != null) {
            return (Usuario) session.getAttribute("usuarioLogado");
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
            Medico medico = getMedicoService().consultar(medicoId);
            
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
            
            // Remove o vínculo da coleção da clínica usando o método auxiliar
            clinica.removerMedicoClinica(vinculoParaRemover);

            // Atualiza a clínica (merge)
            this.alterar(clinica);
        } catch (Exception e) {
            throw new DAOException("Erro ao desvincular médico da clínica: " + e.getMessage(), e);
        }
    }

    /**
     * Método específico para remover médico da clínica garantindo contexto de persistência
     */
    public void removerMedicoComContexto(Long clinicaId, Long medicoId) throws Exception {
        try {
            // Remove diretamente via DAO usando DELETE JPQL
            ((br.com.cesarsants.service.MedicoClinicaService)medicoClinicaService).removerVinculo(clinicaId, medicoId);
        } catch (Exception e) {
            throw new DAOException("Erro ao desvincular médico da clínica: " + e.getMessage(), e);
        }
    }

    @Override
    public void excluir(Clinica clinica) throws DAOException {
        if (clinica == null || clinica.getId() == null) {
            throw new DAOException("Clínica inválida para exclusão", null);
        }

        try {
            // Verifica se a clínica tem agendamentos
            IAgendaService agendaService = new br.com.cesarsants.service.AgendaService();
            if (agendaService.clinicaTemAgendamentos(clinica.getId())) {
                throw new DAOException("Não é possível excluir a clínica '" + clinica.getNome() + "' pois ela possui agendamentos registrados no sistema.", null);
            }

            // Se não houver agendamentos, prossegue com a exclusão
            super.excluir(clinica);
        } catch (DAOException e) {
            // Propaga a exceção mantendo a mensagem original
            throw e;
        } catch (Exception e) {
            e.printStackTrace(); // Log do erro para debug
            throw new DAOException("Erro ao excluir clínica: " + e.getMessage(), e);
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

            // Se não houve mudança no número de salas, busca a entidade atual e atualiza seus campos
            if (clinica.getId() != null) {
                Clinica clinicaAtual = super.consultar(clinica.getId());
                if (clinicaAtual != null) {
                    // Atualiza todos os campos da entidade gerenciada
                    clinicaAtual.setNome(clinica.getNome());
                    clinicaAtual.setCnpj(clinica.getCnpj());
                    clinicaAtual.setEndereco(clinica.getEndereco());
                    clinicaAtual.setHorarioAbertura(clinica.getHorarioAbertura());
                    clinicaAtual.setHorarioFechamento(clinica.getHorarioFechamento());
                    clinicaAtual.setNumeroTotalSalas(clinica.getNumeroTotalSalas());
                    
                    // Atualiza as salas se fornecidas
                    if (clinica.getSalas() != null && !clinica.getSalas().isEmpty()) {
                        // Atualiza as salas existentes
                        for (Sala salaClinica : clinica.getSalas()) {
                            Sala salaAtual = clinicaAtual.getSalas().stream()
                                .filter(s -> s.getId().equals(salaClinica.getId()))
                                .findFirst()
                                .orElse(null);
                            
                            if (salaAtual != null) {
                                salaAtual.setNome(salaClinica.getNome());
                                salaAtual.setOrdem(salaClinica.getOrdem());
                            }
                        }
                    }
                    
                    // Faz o merge da entidade gerenciada
                    return super.alterar(clinicaAtual);
                }
            }

            // Fallback: se não conseguiu buscar a entidade atual, usa o merge normal
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
                // Verifica se as salas que serão excluídas têm agendamentos
                List<Sala> salasParaExcluir = salasAtuais.subList(novoNumeroTotalSalas, salasAtuais.size());
                
                // Verifica cada sala que será excluída
                for (Sala sala : salasParaExcluir) {
                    boolean temAgendamentos = new br.com.cesarsants.service.AgendaService().salaTemAgendamentos(sala.getId());
                    
                    if (temAgendamentos) {
                        throw new DAOException("Não é possível reduzir o número de salas. A sala '" + 
                                             sala.getNome() + "' possui agendamentos registrados no sistema.", null);
                    }
                }
                
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