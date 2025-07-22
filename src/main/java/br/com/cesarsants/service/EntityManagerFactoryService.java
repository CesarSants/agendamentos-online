package br.com.cesarsants.service;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * Serviço para gerenciar o EntityManagerFactory de forma dinâmica
 * Carrega configurações do arquivo database-config.properties
 */
public class EntityManagerFactoryService {
    
    private static final Logger logger = Logger.getLogger(EntityManagerFactoryService.class.getName());
    
    private static EntityManagerFactory emf;
    private static final Object lock = new Object();
    
    /**
     * Obtém o EntityManagerFactory configurado dinamicamente
     */
    public static EntityManagerFactory getEntityManagerFactory() {
        if (emf == null) {
            synchronized (lock) {
                if (emf == null) {
                    emf = createEntityManagerFactory();
                }
            }
        }
        return emf;
    }
    
    /**
     * Cria o EntityManagerFactory com configurações do arquivo de propriedades
     */
    private static EntityManagerFactory createEntityManagerFactory() {
        try {
            DatabaseConfigService configService = new DatabaseConfigService();
            
            if (!configService.isConfigured()) {
                logger.severe("DatabaseConfigService não está configurado. Verifique o arquivo database-config.properties");
                throw new RuntimeException("Configuração de banco de dados não encontrada");
            }
            
            Map<String, Object> properties = new HashMap<>();
            
            // Configurações de conexão
            properties.put("javax.persistence.jdbc.driver", "org.postgresql.Driver");
            properties.put("javax.persistence.jdbc.url", configService.getJdbcUrl());
            properties.put("javax.persistence.jdbc.user", configService.getJdbcUser());
            properties.put("javax.persistence.jdbc.password", configService.getJdbcPassword());
            
            // Configurações básicas do Hibernate
            properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            properties.put("hibernate.show_sql", "true");
            properties.put("hibernate.format_sql", "true");
            properties.put("hibernate.cache.use_second_level_cache", "false");
            properties.put("hibernate.hbm2ddl.auto", "update");
            properties.put("javax.persistence.schema-generation.database.action", "update");
            properties.put("javax.persistence.schema-generation.create-source", "metadata");
            properties.put("javax.persistence.schema-generation.drop-source", "metadata");
            
            // Sobrescreve o provider de conexão para usar HikariCP
            properties.put("hibernate.connection.provider_class", "com.zaxxer.hikari.hibernate.HikariConnectionProvider");
            properties.put("hibernate.hikari.minimumIdle", "3");
            properties.put("hibernate.hikari.maximumPoolSize", "10");
            properties.put("hibernate.hikari.idleTimeout", "30000");
            properties.put("hibernate.hikari.connectionTimeout", "20000");
            properties.put("hibernate.hikari.maxLifetime", "1800000");
            properties.put("hibernate.hikari.poolName", "HikariCP");
            
            logger.info("Criando EntityManagerFactory com configurações dinâmicas");
            logger.info("URL: " + configService.getJdbcUrl());
            logger.info("User: " + configService.getJdbcUser());
            
            EntityManagerFactory factory = Persistence.createEntityManagerFactory("prod", properties);
            logger.info("EntityManagerFactory criado com sucesso");
            
            return factory;
            
        } catch (Exception e) {
            logger.severe("Erro ao criar EntityManagerFactory: " + e.getMessage());
            throw new RuntimeException("Erro ao criar EntityManagerFactory", e);
        }
    }
    
    /**
     * Fecha o EntityManagerFactory
     */
    public static void closeEntityManagerFactory() {
        if (emf != null && emf.isOpen()) {
            emf.close();
            emf = null;
        }
    }
} 