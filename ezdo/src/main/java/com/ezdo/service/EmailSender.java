package com.ezdo.service;

public interface EmailSender {

    void send(String toEmail, String subject, String htmlBody);
}
