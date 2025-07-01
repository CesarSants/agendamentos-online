package br.com.cesarsants;

import java.io.File;

import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import br.com.cesarsants.utils.DatabasePropertiesLoader;

/**
 * @author cesarsants
 *
 */

public class Main {
    public static void main(String[] args) throws Exception {
        // Inicializar propriedades do banco de dados
        DatabasePropertiesLoader loader = new DatabasePropertiesLoader();
        loader.contextInitialized(null);
        
        // Configurar Tomcat
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(Integer.parseInt(System.getenv().getOrDefault("PORT", "8080")));
        
        // Configurar contexto da aplicação
        StandardContext ctx = (StandardContext) tomcat.addWebapp("", new File("src/main/webapp").getAbsolutePath());
        ctx.setReloadable(false);
        
        // Configurar recursos
        File additionWebInfClasses = new File("target/classes");
        WebResourceRoot resources = new StandardRoot(ctx);
        resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes", additionWebInfClasses.getAbsolutePath(), "/"));
        ctx.setResources(resources);
        
        // Iniciar Tomcat
        tomcat.start();
        tomcat.getServer().await();
    }
} 