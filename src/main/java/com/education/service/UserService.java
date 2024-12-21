package com.education.service;

import com.education.model.dto.UserDto;
import com.education.model.entity.User;
import com.education.model.entity.VerificationToken;

public interface UserService {
    User signup(UserDto accountDto);
    void addUserLocation(User user, String ip);
    void createVerificationTokenForUser(User user, String token);
    VerificationToken generateNewVerificationToken(String token);
    User getUser(String verificationToken);
    VerificationToken getVerificationToken(String VerificationToken);
    void saveRegisteredUser(User user);
}
