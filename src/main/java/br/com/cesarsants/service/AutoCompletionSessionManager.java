package br.com.cesarsants.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * Gerenciador central de sessões para o sistema de atualização automática.
 * Controla quais usuários têm o sistema de atualização automática ativo
 * e gerencia o estado global do scheduler.
 */

/**
 * @author cesarsants
 *
 */

@Singleton
@Startup
public class AutoCompletionSessionManager {
    
    // Set thread-safe para armazenar os IDs dos usuários com sistema ativo
    private final Set<Long> usuariosAtivos = ConcurrentHashMap.newKeySet();
    
    // Flag para controlar se o scheduler está ativo globalmente
    private final AtomicBoolean schedulerAtivo = new AtomicBoolean(false);
    
    /**
     * Adiciona um usuário ao conjunto de usuários com sistema ativo
     * @param usuarioId ID do usuário
     * @return true se o usuário foi adicionado, false se já estava presente
     */
    public boolean adicionarUsuarioAtivo(Long usuarioId) {
        if (usuarioId == null) {
            return false;
        }
        
        boolean adicionado = usuariosAtivos.add(usuarioId);
        
        // Se foi adicionado e o scheduler não estava ativo, ativa o scheduler
        if (adicionado && !schedulerAtivo.get()) {
            schedulerAtivo.set(true);
            System.out.println("Scheduler de atualização automática ativado. Usuários ativos: " + usuariosAtivos.size());
        }
        
        System.out.println("Usuário " + usuarioId + " adicionado ao sistema de atualização automática. " +
                          "Total de usuários ativos: " + usuariosAtivos.size());
        
        return adicionado;
    }
    
    /**
     * Remove um usuário do conjunto de usuários com sistema ativo
     * @param usuarioId ID do usuário
     * @return true se o usuário foi removido, false se não estava presente
     */
    public boolean removerUsuarioAtivo(Long usuarioId) {
        if (usuarioId == null) {
            return false;
        }
        
        boolean removido = usuariosAtivos.remove(usuarioId);
        
        // Se foi removido e não há mais usuários ativos, desativa o scheduler
        if (removido && usuariosAtivos.isEmpty()) {
            schedulerAtivo.set(false);
            System.out.println("Scheduler de atualização automática desativado. Nenhum usuário ativo.");
        }
        
        System.out.println("Usuário " + usuarioId + " removido do sistema de atualização automática. " +
                          "Total de usuários ativos: " + usuariosAtivos.size());
        
        return removido;
    }
    
    /**
     * Verifica se um usuário específico tem o sistema ativo
     * @param usuarioId ID do usuário
     * @return true se o usuário tem o sistema ativo
     */
    public boolean isUsuarioAtivo(Long usuarioId) {
        return usuarioId != null && usuariosAtivos.contains(usuarioId);
    }
    
    /**
     * Verifica se o scheduler está ativo globalmente
     * @return true se o scheduler está ativo
     */
    public boolean isSchedulerAtivo() {
        return schedulerAtivo.get();
    }
    
    /**
     * Retorna o conjunto de usuários ativos
     * @return Set com os IDs dos usuários ativos
     */
    public Set<Long> getUsuariosAtivos() {
        return usuariosAtivos;
    }
    
    /**
     * Retorna a quantidade de usuários ativos
     * @return número de usuários ativos
     */
    public int getQuantidadeUsuariosAtivos() {
        return usuariosAtivos.size();
    }
    
    /**
     * Verifica se há usuários ativos
     * @return true se há pelo menos um usuário ativo
     */
    public boolean hasUsuariosAtivos() {
        return !usuariosAtivos.isEmpty();
    }
} 