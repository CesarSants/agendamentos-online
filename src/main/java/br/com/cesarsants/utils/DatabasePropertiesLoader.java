package br.com.cesarsants.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * @author cesarsants
 *
 */

@WebListener
public class DatabasePropertiesLoader implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        Properties props = new Properties();
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("db-prod.properties")) {
            if (input != null) {
                props.load(input);
                System.setProperty("JDBC_DATABASE_URL", props.getProperty("jdbc.url"));
                System.setProperty("JDBC_DATABASE_USER", props.getProperty("jdbc.user"));
                System.setProperty("JDBC_DATABASE_PASSWORD", props.getProperty("jdbc.password"));
            } else {
                System.err.println("Arquivo db-prod.properties não encontrado!");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Nada a fazer
    }
} 