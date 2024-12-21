package com.education.service.impl;

import com.education.service.MailSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

import static com.education.util.CommonUtil.functionSendMail;

@Component
public class JavaMailSenderImpl implements MailSender {

    @Autowired
    private Environment env;

    @Override
    public void send(SimpleMailMessage email) throws MailException {
org.springframework.mail.javamail.JavaMailSenderImpl mailSender = new org.springframework.mail.javamail.JavaMailSenderImpl();
        functionSendMail(mailSender, email, env);
    }
}
