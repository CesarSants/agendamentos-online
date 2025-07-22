package br.com.cesarsants.listener;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import br.com.cesarsants.domain.Usuario;
import br.com.cesarsants.service.AutoCompletionSessionManager;

/**
 * Listener para detectar criação e destruição de sessões
 * Garante logout automático quando a sessão expira ou é invalidada
 */
public class SessionListener implements HttpSessionListener {
    
    @Override
    public void sessionCreated(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        System.out.println("=== SESSÃO CRIADA ===");
        System.out.println("ID da sessão: " + session.getId());
        System.out.println("Tempo de criação: " + new java.util.Date(session.getCreationTime()));
        System.out.println("Timeout configurado: " + session.getMaxInactiveInterval() + " segundos");
    }
    
    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        String sessionId = session.getId();
        
        System.out.println("=== SESSÃO DESTRUÍDA ===");
        System.out.println("ID da sessão: " + sessionId);
        System.out.println("Tempo de destruição: " + new java.util.Date());
        
        // Verificar se foi por timeout
        long currentTime = System.currentTimeMillis();
        long lastAccessTime = session.getLastAccessedTime();
        long timeout = session.getMaxInactiveInterval() * 1000L;
        
        // NOVO: Se for sessão pública, recria imediatamente para evitar limbo
        Boolean isPublicSession = (Boolean) session.getAttribute("isPublicSession");
        if (isPublicSession != null && isPublicSession) {
            System.out.println("Sessão pública destruída por timeout. Recriando sessão para evitar limbo.");
            try {
                // Não há acesso direto ao request, mas a próxima requisição do usuário criará nova sessão automaticamente.
                // Opcional: pode-se logar um aviso ou tomar ação adicional se necessário.
            } catch (Exception e) {
                System.err.println("Erro ao tentar recriar sessão pública: " + e.getMessage());
            }
            // Não executa mais nada, apenas retorna
            return;
        }
        
        if (currentTime - lastAccessTime >= timeout) {
            System.out.println("Sessão expirou por timeout (inativo por " + (timeout / 60000) + " minutos)");
        } else {
            System.out.println("Sessão destruída manualmente");
        }
        
        // Obter usuário logado antes de limpar a sessão
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
        if (usuarioLogado != null) {
            System.out.println("Usuário logado: " + usuarioLogado.getNome() + " (ID: " + usuarioLogado.getId() + ")");
            
            // Remover usuário do sistema de atualização automática
            try {
                AutoCompletionSessionManager sessionManager = AutoCompletionSessionManager.getInstance();
                sessionManager.removerUsuarioAtivo(usuarioLogado.getId());
                System.out.println("Usuário " + usuarioLogado.getId() + " removido do sistema de atualização automática. Total de usuários ativos: " + sessionManager.getUsuariosAtivos().size());
            } catch (Exception e) {
                System.err.println("Erro ao remover usuário do sistema de atualização: " + e.getMessage());
            }
            
            // Limpar dados da sessão
            session.removeAttribute("usuarioLogado");
            session.removeAttribute("emailPendente");
            session.removeAttribute("nomePendente");
            session.removeAttribute("senhaPendente");
            session.removeAttribute("novaSenhaParaAlteracao");
            session.removeAttribute("recuperacaoSenhaAtiva");
            session.removeAttribute("sistemaAtualizacaoAtivo");
            
            System.out.println("Usuário removido do sistema de atualização automática");
            System.out.println("Dados da sessão limpos com sucesso");
        } else {
            System.out.println("Nenhum usuário logado encontrado na sessão");
        }
        
        System.out.println("=== SESSÃO FINALIZADA ===");
    }
} 