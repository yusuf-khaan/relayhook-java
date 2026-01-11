package com.app.relayhook.Integrations.Mail;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MailClient implements MailAbs {
    private final JavaMailSender mailSender;
    private final Environment env;
    private final TemplateEngine templateEngine;

    @Autowired
    public MailClient(JavaMailSender mailSender, Environment env,
            TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.env = env;
        this.templateEngine = templateEngine;
    }

    @Async
    public CompletableFuture<Map<String, String>> sendTemplateMail(
            String to[], String subject, String templateName,
            Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);
            String body = templateEngine.process(templateName, context);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom(env.getProperty("spring.mail.frommail"));
            helper.setText(body, true);
            mailSender.send(message);
            return CompletableFuture.completedFuture(Map.of("status", "success", "code", "200"));

        } catch (Exception e) {
            return CompletableFuture.completedFuture(Map.of("status", e.getMessage(), "code", "400"));
        }
    }

}
