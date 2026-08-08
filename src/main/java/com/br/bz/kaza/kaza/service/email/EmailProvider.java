package com.br.bz.kaza.kaza.service.email;

public interface EmailProvider {
    void send(EmailMessage message);

    record EmailMessage(String from, String to, String subject, String body) {
    }
}
