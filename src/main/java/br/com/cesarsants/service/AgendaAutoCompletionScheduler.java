package br.com.cesarsants.service;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import br.com.cesarsants.exceptions.BusinessException;

/**
 * @author cesarsants
 *
 */

/**
 * Scheduler responsável por verificar e concluir automaticamente agendamentos
 * que já passaram do horário de fim da consulta.
 * 
 * Executa a cada 1 minuto para verificar agendamentos que devem ser concluídos.
 * Só executa se houver usuários com o sistema de atualização automática ativo.
 */
@Singleton
@Startup
public class AgendaAutoCompletionScheduler {
    
    private static final int INTERVALO_VERIFICACAO_MINUTOS = 1; // Verifica a cada 1 minuto
    
    @Inject
    private IAgendaService agendaService;
    
    @Inject
    private AutoCompletionSessionManager sessionManager;
    
    private ScheduledExecutorService scheduler;
    
    @PostConstruct
    public void inicializar() {
        System.out.println("Iniciando AgendaAutoCompletionScheduler às " + LocalDateTime.now());
        
        // Cria um scheduler com um thread dedicado
        scheduler = Executors.newScheduledThreadPool(1);
        
        // Agenda a tarefa para executar a cada INTERVALO_VERIFICACAO_MINUTOS minutos
        scheduler.scheduleAtFixedRate(
            this::executarVerificacaoAutomatica,
            0, // Executa imediatamente na primeira vez
            INTERVALO_VERIFICACAO_MINUTOS,
            TimeUnit.MINUTES
        );
        
        System.out.println("AgendaAutoCompletionScheduler iniciado com sucesso. " +
                          "Verificações a cada " + INTERVALO_VERIFICACAO_MINUTOS + " minutos.");
    }
    
    @PreDestroy
    public void finalizar() {
        System.out.println("Finalizando AgendaAutoCompletionScheduler às " + LocalDateTime.now());
        
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("AgendaAutoCompletionScheduler finalizado.");
    }
    
    /**
     * Método que executa a verificação automática de agendamentos para conclusão
     * Só executa se houver usuários com o sistema de atualização automática ativo
     */
    private void executarVerificacaoAutomatica() {
        try {
            // Verifica se há usuários ativos antes de executar
            if (!sessionManager.hasUsuariosAtivos()) {
                System.out.println("Nenhum usuário com sistema de atualização automática ativo. " +
                                 "Pulando verificação automática às " + LocalDateTime.now());
                return;
            }
            
            System.out.println("Executando verificação automática de agendamentos às " + LocalDateTime.now() + 
                             " para " + sessionManager.getQuantidadeUsuariosAtivos() + " usuário(s) ativo(s)");
            
            // Busca agendamentos que devem ser concluídos automaticamente
            var agendamentosParaConcluir = agendaService.buscarAgendamentosParaConclusaoAutomatica();
            
            if (!agendamentosParaConcluir.isEmpty()) {
                System.out.println("Encontrados " + agendamentosParaConcluir.size() + 
                                 " agendamentos para conclusão automática");
                
                // Executa a conclusão automática
                agendaService.concluirAgendamentosAutomaticamente();
                
                System.out.println("Verificação automática concluída com sucesso às " + LocalDateTime.now());
            } else {
                System.out.println("Nenhum agendamento encontrado para conclusão automática às " + LocalDateTime.now());
            }
            
        } catch (BusinessException e) {
            System.err.println("Erro durante verificação automática de agendamentos: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Erro inesperado durante verificação automática de agendamentos: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Método público para executar verificação manual (útil para testes)
     */
    public void executarVerificacaoManual() {
        executarVerificacaoAutomatica();
    }
} 