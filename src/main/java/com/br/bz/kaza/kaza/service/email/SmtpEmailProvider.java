package com.br.bz.kaza.kaza.service.email;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpEmailProvider implements EmailProvider {
    private final JavaMailSender mailSender;

    public SmtpEmailProvider(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void send(EmailMessage message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(message.from());
        mail.setTo(message.to());
        mail.setSubject(message.subject());
        mail.setText(message.body());
        mailSender.send(mail);
    }
}
