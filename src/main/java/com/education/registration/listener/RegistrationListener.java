package com.education.registration.listener;


import com.education.model.entity.User;
import com.education.registration.OnRegistrationCompleteEvent;
import com.education.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static com.education.util.CommonUtil.functionSendMail;

/**
 * Class cấu hình năng nghe sự kiện đăng ký hoàn thành
 */
@Component
public class RegistrationListener implements ApplicationListener<OnRegistrationCompleteEvent> {
    @Autowired
    private UserService service;

    @Autowired
    private MessageSource messages;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private Environment env;

    // API

    /**
     * hàm thực thi mặc định của ApplicationListener
     * @param event là sự kiện tự định nghĩa - OnRegistrationCompleteEvent
     */
    @Override
    public void onApplicationEvent(final OnRegistrationCompleteEvent event) {
        this.confirmRegistration(event);
    }

    /**
     * hàm gửi email xác nhận
     * @param event là sự kiện tự định nghĩa - OnRegistrationCompleteEvent
     */
    private void confirmRegistration(final OnRegistrationCompleteEvent event) {
        final User user = event.getUser();
        final String token = UUID.randomUUID().toString();
        service.createVerificationTokenForUser(user, token);

        final SimpleMailMessage email = constructEmailMessage(event, user, token);
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        functionSendMail(mailSender, email, env);
    }

    /**
     * @param event là sự kiện tự định nghĩa - OnRegistrationCompleteEvent
     * @param user là thông tin người dùng
     * @param token là mã thông báo
     * @return hàm trả về SimpleMailMessage là nội dung email
     */
    private SimpleMailMessage constructEmailMessage(final OnRegistrationCompleteEvent event, final User user, final String token) {
        final String recipientAddress = user.getEmail();
        final String subject = "Registration Confirmation";
        final String confirmationUrl = event.getAppUrl() + "/api/registrationConfirm?token=" + token;
        final String message = messages.getMessage("message.regSuccLink", null, "You registered successfully. To confirm your registration, please click on the below link.", event.getLocale());
        final SimpleMailMessage email = new SimpleMailMessage();

        try {

            // Tạo một tin nhắn email

            email.setFrom("hungbv115@gmail.com");
            email.setTo("buihung11597@gmail.com");
            email.setSubject(subject);
            email.setText(message + " \r\n" + confirmationUrl);

            // Log thông báo
            System.out.println("Email sent successfully!");
        } catch (Exception e) {
            // Log lỗi nếu có
            System.err.println("Error sending email: " + e.getMessage());
            // Throw exception để công việc thất bại nếu cần
            throw new RuntimeException("Error sending email", e);
        }
        return email;
    }
    

}
