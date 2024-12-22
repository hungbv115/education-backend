package com.education.controller;

import com.education.model.dto.*;
import com.education.model.entity.User;
import com.education.model.entity.VerificationToken;
import com.education.registration.OnRegistrationCompleteEvent;
import com.education.security.HandleJWT;
import com.education.service.MailSender;
import com.education.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Calendar;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final MessageSource messages;
    @Autowired
    private Environment env;
    @Autowired
    private MailSender mailSender;
    private final HandleJWT handleJWT;

    public AuthController(AuthenticationManager authenticationManager, UserService userService, ApplicationEventPublisher eventPublisher, MessageSource messages, HandleJWT handleJWT) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
        this.messages = messages;
        this.handleJWT = handleJWT;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody AuthRequest request) throws Throwable {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
            String token = handleJWT.generateToken(request.getUsername());
            return ResponseEntity.ok(new LoginResponse(request.getUsername(), token, "SUCCESS"));
        } catch (AuthenticationException e) {
//            e.printStackTrace();
//            throw e.getCause();
            return ResponseEntity.badRequest().body(new LoginResponse(request.getUsername(), "", "FAIL"));
        }

    }

    @Operation(summary = "Signup user")
    @ApiResponse(responseCode = "201")
    @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid final UserDto accountDto, final HttpServletRequest request) {
        final User registered = userService.signup(accountDto);
        eventPublisher.publishEvent(new OnRegistrationCompleteEvent(registered, request.getLocale(), getAppUrl(request)));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // User activation - verification
    @GetMapping("/user/resendRegistrationToken")
    public ResponseEntity<Void> resendRegistrationToken(final HttpServletRequest request, @RequestParam("token") final String existingToken) {

        final VerificationToken newToken = userService.generateNewVerificationToken(existingToken);
        final User user = userService.getUser(newToken.getToken());
        mailSender.send(constructResendVerificationTokenEmail(getAppUrl(request), request.getLocale(), newToken, user));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/registrationConfirm")
    public String confirmRegistration(final HttpServletRequest request, @RequestParam("token") final String token) {
        final Locale locale = request.getLocale();
        final VerificationToken verificationToken = userService.getVerificationToken(token);
        if (verificationToken == null) {
            return messages.getMessage("auth.message.invalidToken", null, locale);
        }

        final User user = verificationToken.getUser();
        final Calendar cal = Calendar.getInstance();
        if ((verificationToken.getExpiryDate().getTime() - cal.getTime().getTime()) <= 0) {
            return  messages.getMessage("auth.message.expired", null, locale);
        }

        user.setEnabled(true);
        userService.saveRegisteredUser(user);
        return messages.getMessage("message.accountVerified", null, locale);
    }

    // Reset password
    @PostMapping("/user/resetPassword")
    public ResponseEntity<String> resetPassword(final HttpServletRequest request, @RequestParam("email") final String userEmail) {
        final User user = userService.findUserByEmail(userEmail);
        if (user != null) {
            final String token = UUID.randomUUID().toString();
            userService.createPasswordResetTokenForUser(user, token);
            mailSender.send(constructResetTokenEmail(getAppUrl(request), request.getLocale(), token, user));
        }

        return ResponseEntity.ok(messages.getMessage("message.resetPasswordEmail", null, request.getLocale()));
    }

    // Save password
    @PostMapping("/user/savePassword")
    public ResponseEntity<String> savePassword(final Locale locale, @Valid PasswordDto passwordDto) {

        final String result = userService.validatePasswordResetToken(passwordDto.getToken());

        if(result != null) {
            return ResponseEntity.ok(messages.getMessage("auth.message." + result, null, locale));
        }

        Optional<User> user = userService.getUserByPasswordResetToken(passwordDto.getToken());
        if(user.isPresent()) {
            userService.changeUserPassword(user.get(), passwordDto.getNewPassword(), passwordDto.getToken());
            return ResponseEntity.ok(messages.getMessage("message.resetPasswordSuc", null, locale));
        } else {
            return ResponseEntity.badRequest().body(messages.getMessage("auth.message.invalid", null, locale));
        }
    }

    private SimpleMailMessage constructResendVerificationTokenEmail(final String contextPath, final Locale locale, final VerificationToken newToken, final User user) {
        final String confirmationUrl = contextPath + "/api/registrationConfirm?token=" + newToken.getToken();
        final String message = messages.getMessage("message.resendToken", null, locale);
        return constructEmail("Resend Registration Token", message + " \r\n" + confirmationUrl, user);
    }

    private SimpleMailMessage constructResetTokenEmail(final String contextPath, final Locale locale, final String token, final User user) {
        final String url = contextPath + "/api/user/changePassword?token=" + token;
        final String message = messages.getMessage("message.resetPassword", null, locale);
        return constructEmail("Reset Password", message + " \r\n" + url, user);
    }

    private SimpleMailMessage constructEmail(String subject, String body, User user) {
        final SimpleMailMessage email = new SimpleMailMessage();
        email.setSubject(subject);
        email.setText(body);
        email.setTo(user.getEmail());
        email.setFrom(env.getProperty("support.email"));
        return email;
    }

    private String getAppUrl(HttpServletRequest request) {
        return "http://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
    }
}