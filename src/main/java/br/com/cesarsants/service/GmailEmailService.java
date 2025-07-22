package br.com.cesarsants.service;

import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class GmailEmailService {
    
    private static final Logger logger = Logger.getLogger(GmailEmailService.class.getName());
    
    private String smtpHost;
    private String smtpPort;
    private String username;
    private String password;
    private boolean configured = false;
    
    public GmailEmailService() {
        loadConfiguration();
    }
    
    /**
     * Carrega configurações de forma segura do arquivo externo
     */
    private void loadConfiguration() {
        try {
            // Primeiro tenta ler das variáveis de ambiente
            String envHost = System.getenv("GMAIL_SMTP_HOST");
            String envPort = System.getenv("GMAIL_SMTP_PORT");
            String envUser = System.getenv("GMAIL_USERNAME");
            String envPass = System.getenv("GMAIL_PASSWORD");
            if (envHost != null && envPort != null && envUser != null && envPass != null) {
                smtpHost = envHost;
                smtpPort = envPort;
                username = envUser;
                password = envPass;
                configured = true;
                logger.info("Configuração de email carregada das variáveis de ambiente do sistema");
            } else {
                Properties props = new Properties();
                try (InputStream input = getClass().getClassLoader().getResourceAsStream("email-config.properties")) {
                    if (input != null) {
                        props.load(input);
                        logger.info("Configuração carregada de email-config.properties");
                        smtpHost = props.getProperty("gmail.smtp.host");
                        smtpPort = props.getProperty("gmail.smtp.port");
                        username = props.getProperty("gmail.username");
                        password = props.getProperty("gmail.password");
                        if (smtpHost != null && smtpPort != null && username != null && password != null) {
                            configured = true;
                            logger.info("GmailEmailService configurado com sucesso");
                        } else {
                            logger.warning("Configurações de email incompletas no arquivo email-config.properties");
                        }
                    } else {
                        logger.warning("Arquivo email-config.properties não encontrado. Crie este arquivo em src/main/resources/ ou defina as variáveis de ambiente GMAIL_SMTP_HOST, GMAIL_SMTP_PORT, GMAIL_USERNAME, GMAIL_PASSWORD");
                    }
                }
            }
        } catch (Exception e) {
            logger.severe("Erro ao carregar configurações de email: " + e.getMessage());
        }
    }
    
    /**
     * Envia email usando Gmail SMTP
     */
    public boolean enviarEmail(String toEmail, String subject, String htmlContent, String textContent) {
        if (!configured) {
            logger.severe("GmailEmailService não está configurado. Verifique o arquivo email-config.properties");
            return false;
        }
        
        try {
            // Configurar propriedades do JavaMail
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", smtpPort);
            
            // Criar sessão
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
            
            // Criar mensagem
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username, "Sistema de Agendamento"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            
            // Configurar conteúdo multipart (HTML + texto)
            Multipart multipart = new MimeMultipart("alternative");
            
            // Parte texto
            BodyPart textPart = new MimeBodyPart();
            textPart.setText(textContent);
            multipart.addBodyPart(textPart);
            
            // Parte HTML
            BodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlContent, "text/html; charset=UTF-8");
            multipart.addBodyPart(htmlPart);
            
            message.setContent(multipart);
            
            // Enviar email
            Transport.send(message);
            
            logger.info("Email enviado com sucesso para: " + toEmail);
            return true;
            
        } catch (Exception e) {
            logger.severe("Erro ao enviar email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Envia email de confirmação de cadastro
     */
    public boolean enviarEmailConfirmacao(String email, String codigo) {
        String subject = "Confirmação de Cadastro - Sistema de Agendamento";
        String htmlContent = gerarTemplateConfirmacaoHTML(codigo);
        String textContent = gerarTemplateConfirmacaoTexto(codigo);
        
        return enviarEmail(email, subject, htmlContent, textContent);
    }
    
    /**
     * Envia email de recuperação de senha
     */
    public boolean enviarEmailRecuperacaoSenha(String email, String codigo) {
        String subject = "Recuperação de Senha - Sistema de Agendamento";
        String htmlContent = gerarTemplateRecuperacaoHTML(codigo);
        String textContent = gerarTemplateRecuperacaoTexto(codigo);
        
        return enviarEmail(email, subject, htmlContent, textContent);
    }
    
    /**
     * Template HTML para confirmação de cadastro
     */
    private String gerarTemplateConfirmacaoHTML(String codigo) {
        return "<!DOCTYPE html>" +
               "<html>" +
               "<head>" +
               "<meta charset='UTF-8'>" +
               "<title>Confirmação de Cadastro</title>" +
               "<style>" +
               "body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f4f4f4; }" +
               ".container { max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }" +
               ".header { text-align: center; margin-bottom: 30px; }" +
               ".header h1 { color: #667eea; margin: 0; }" +
               ".code { background-color: #667eea; color: white; padding: 15px; border-radius: 8px; text-align: center; font-size: 24px; font-weight: bold; margin: 20px 0; }" +
               ".footer { text-align: center; margin-top: 30px; color: #666; font-size: 14px; }" +
               "</style>" +
               "</head>" +
               "<body>" +
               "<div class='container'>" +
               "<div class='header'>" +
               "<h1>Sistema de Agendamento</h1>" +
               "<p>Confirmação de Cadastro</p>" +
               "</div>" +
               "<p>Olá!</p>" +
               "<p>Obrigado por se cadastrar no nosso sistema. Para completar seu cadastro, use o código de confirmação abaixo:</p>" +
               "<div class='code'>" + codigo + "</div>" +
               "<p><strong>Importante:</strong></p>" +
               "<ul>" +
               "<li>Este código expira em 15 minutos</li>" +
               "<li>Não compartilhe este código com ninguém</li>" +
               "<li>Se você não solicitou este cadastro, ignore este email</li>" +
               "</ul>" +
               "<div class='footer'>" +
               "<p>Este é um email automático, não responda a esta mensagem.</p>" +
               "</div>" +
               "</div>" +
               "</body>" +
               "</html>";
    }
    
    /**
     * Template texto para confirmação de cadastro
     */
    private String gerarTemplateConfirmacaoTexto(String codigo) {
        return "Sistema de Agendamento - Confirmação de Cadastro\n\n" +
               "Olá!\n\n" +
               "Obrigado por se cadastrar no nosso sistema. Para completar seu cadastro, use o código de confirmação abaixo:\n\n" +
               "CÓDIGO: " + codigo + "\n\n" +
               "Importante:\n" +
               "- Este código expira em 15 minutos\n" +
               "- Não compartilhe este código com ninguém\n" +
               "- Se você não solicitou este cadastro, ignore este email\n\n" +
               "Este é um email automático, não responda a esta mensagem.";
    }
    
    /**
     * Template HTML para recuperação de senha
     */
    private String gerarTemplateRecuperacaoHTML(String codigo) {
        return "<!DOCTYPE html>" +
               "<html>" +
               "<head>" +
               "<meta charset='UTF-8'>" +
               "<title>Recuperação de Senha</title>" +
               "<style>" +
               "body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f4f4f4; }" +
               ".container { max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }" +
               ".header { text-align: center; margin-bottom: 30px; }" +
               ".header h1 { color: #667eea; margin: 0; }" +
               ".code { background-color: #667eea; color: white; padding: 15px; border-radius: 8px; text-align: center; font-size: 24px; font-weight: bold; margin: 20px 0; }" +
               ".footer { text-align: center; margin-top: 30px; color: #666; font-size: 14px; }" +
               "</style>" +
               "</head>" +
               "<body>" +
               "<div class='container'>" +
               "<div class='header'>" +
               "<h1>Sistema de Agendamento</h1>" +
               "<p>Recuperação de Senha</p>" +
               "</div>" +
               "<p>Olá!</p>" +
               "<p>Você solicitou a recuperação de senha da sua conta. Use o código de confirmação abaixo para alterar sua senha:</p>" +
               "<div class='code'>" + codigo + "</div>" +
               "<p><strong>Importante:</strong></p>" +
               "<ul>" +
               "<li>Este código expira em 15 minutos</li>" +
               "<li>Não compartilhe este código com ninguém</li>" +
               "<li>Se você não solicitou esta recuperação, ignore este email</li>" +
               "</ul>" +
               "<div class='footer'>" +
               "<p>Este é um email automático, não responda a esta mensagem.</p>" +
               "</div>" +
               "</div>" +
               "</body>" +
               "</html>";
    }
    
    /**
     * Template texto para recuperação de senha
     */
    private String gerarTemplateRecuperacaoTexto(String codigo) {
        return "Sistema de Agendamento - Recuperação de Senha\n\n" +
               "Olá!\n\n" +
               "Você solicitou a recuperação de senha da sua conta. Use o código de confirmação abaixo para alterar sua senha:\n\n" +
               "CÓDIGO: " + codigo + "\n\n" +
               "Importante:\n" +
               "- Este código expira em 15 minutos\n" +
               "- Não compartilhe este código com ninguém\n" +
               "- Se você não solicitou esta recuperação, ignore este email\n\n" +
               "Este é um email automático, não responda a esta mensagem.";
    }
} 