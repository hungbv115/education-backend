package com.education.util;

import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

public class CommonUtil {
    /**
     * @param mailSender là hàm thực hiện gửi email từ thư viện javamail
     * @param email là nội dung email
     * @param env là giao diện lấy thông tin cấu hình
     */
    public static void functionSendMail(JavaMailSenderImpl mailSender, SimpleMailMessage email, Environment env) {
        mailSender.setHost(env.getProperty("spring.mail.host"));
        mailSender.setPort(Integer.parseInt(env.getProperty("spring.mail.port")));

        mailSender.setUsername(env.getProperty("spring.mail.username"));
        mailSender.setPassword(env.getProperty("spring.mail.password"));

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.debug", "true");
        mailSender.send(email);
    }
}
