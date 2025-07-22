package br.com.cesarsants.service;

import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Serviço para carregar configurações do banco de dados de forma segura
 */
public class DatabaseConfigService {
    
    private static final Logger logger = Logger.getLogger(DatabaseConfigService.class.getName());
    
    private Properties databaseProps;
    private boolean configured = false;
    
    public DatabaseConfigService() {
        loadConfiguration();
    }
    
    /**
     * Carrega configurações de forma segura do arquivo externo
     */
    private void loadConfiguration() {
        try {
            databaseProps = new Properties();
            // Primeiro tenta ler das variáveis de ambiente
            String url = System.getenv("NEON_JDBC_URL");
            String user = System.getenv("NEON_JDBC_USER");
            String password = System.getenv("NEON_JDBC_PASSWORD");
            if (url != null && user != null && password != null) {
                databaseProps.setProperty("neon.jdbc.url", url);
                databaseProps.setProperty("neon.jdbc.user", user);
                databaseProps.setProperty("neon.jdbc.password", password);
                configured = true;
                logger.info("Configuração carregada das variáveis de ambiente do sistema");
            } else {
                // Fallback: tenta carregar do arquivo .properties
                try (InputStream input = getClass().getClassLoader().getResourceAsStream("database-config.properties")) {
                    if (input != null) {
                        databaseProps.load(input);
                        logger.info("Configuração carregada de database-config.properties");
                        url = databaseProps.getProperty("neon.jdbc.url");
                        user = databaseProps.getProperty("neon.jdbc.user");
                        password = databaseProps.getProperty("neon.jdbc.password");
                        if (url != null && user != null && password != null) {
                            configured = true;
                            logger.info("DatabaseConfigService configurado com sucesso");
                        } else {
                            logger.warning("Configurações de banco de dados incompletas no arquivo database-config.properties");
                        }
                    } else {
                        logger.warning("Arquivo database-config.properties não encontrado. Crie este arquivo em src/main/resources/ ou defina as variáveis de ambiente NEON_JDBC_URL, NEON_JDBC_USER, NEON_JDBC_PASSWORD");
                    }
                }
            }
        } catch (Exception e) {
            logger.severe("Erro ao carregar configurações de banco de dados: " + e.getMessage());
        }
    }
    
    /**
     * Obtém uma propriedade do arquivo de configuração
     */
    public String getProperty(String key) {
        if (!configured) {
            logger.severe("DatabaseConfigService não está configurado. Verifique o arquivo database-config.properties");
            return null;
        }
        return databaseProps.getProperty(key);
    }
    
    /**
     * Obtém a URL de conexão do banco de dados
     */
    public String getJdbcUrl() {
        return getProperty("neon.jdbc.url");
    }
    
    /**
     * Obtém o usuário do banco de dados
     */
    public String getJdbcUser() {
        return getProperty("neon.jdbc.user");
    }
    
    /**
     * Obtém a senha do banco de dados
     */
    public String getJdbcPassword() {
        return getProperty("neon.jdbc.password");
    }
    
    /**
     * Verifica se o serviço está configurado
     */
    public boolean isConfigured() {
        return configured;
    }
    
    /**
     * Obtém todas as propriedades do banco de dados
     */
    public Properties getDatabaseProperties() {
        return databaseProps;
    }
} 