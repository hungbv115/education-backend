package com.education.service;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;

public interface MailSender {
    void send(SimpleMailMessage simpleMessages) throws MailException;
}
