package com.education.service;

import com.education.exceptions.DuplicateException;
import com.education.model.dto.AuthRequest;
import com.education.model.entity.User;
import com.education.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void signup(AuthRequest request) {
        String email = request.getUsername();
        Optional<User> existingUser = repository.findByUsername(email);
        if (existingUser.isPresent()) {
            throw new DuplicateException(String.format("User with the email address '%s' already exists.", email));
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        repository.insertNewUser(request.getUsername(), hashedPassword);
    }

}
