package com.globalisosecurity.backend.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetMailer {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailer.class);
    private final ObjectProvider<JavaMailSender> senderProvider;
    private final String smtpHost;
    private final String smtpUsername;
    private final String smtpPassword;

    public PasswordResetMailer(ObjectProvider<JavaMailSender> senderProvider,
            @Value("${spring.mail.host:}") String smtpHost,
            @Value("${spring.mail.username:}") String smtpUsername,
            @Value("${spring.mail.password:}") String smtpPassword) {
        this.senderProvider = senderProvider;
        this.smtpHost = smtpHost == null ? "" : smtpHost.trim();
        this.smtpUsername = smtpUsername == null ? "" : smtpUsername.trim();
        this.smtpPassword = smtpPassword == null ? "" : smtpPassword.trim();
    }

    public boolean isConfigured() {
        return !smtpHost.isBlank() && !"localhost".equalsIgnoreCase(smtpHost)
                && !smtpUsername.isBlank() && !smtpPassword.isBlank()
                && senderProvider.getIfAvailable() != null;
    }

    @Async("securityMailExecutor")
    public void sendResetLink(String recipient, String sender, String resetLink) {
        JavaMailSender mailSender = senderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Password reset email could not be queued because SMTP is not configured.");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(sender);
            message.setTo(recipient);
            message.setSubject("Restablecer contraseña de Global ISO Security");
            message.setText("Se solicitó un cambio de contraseña para esta cuenta.\n\n"
                    + "Abra este enlace para elegir una contraseña nueva (válido por 15 minutos y de un solo uso):\n"
                    + resetLink + "\n\nSi no solicitó el cambio, ignore este mensaje.");
            mailSender.send(message);
        } catch (Exception exception) {
            // Do not log the recipient, reset URL, token, or SMTP exception details.
            log.warn("Password reset email delivery failed; check protected mail service logs.");
        }
    }
}
