package com.education.service;

import com.education.model.dto.UserDto;
import com.education.model.entity.DeviceType;
import com.education.model.entity.User;
import com.education.model.entity.VerificationToken;
import com.google.zxing.WriterException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Optional;

public interface UserService {
    User signup(UserDto accountDto);
    void addUserLocation(User user, String ip);
    void createVerificationTokenForUser(User user, String token);
    VerificationToken generateNewVerificationToken(String token);
    User getUser(String verificationToken);
    VerificationToken getVerificationToken(String VerificationToken);
    void saveRegisteredUser(User user);
    User findUserByEmail(String email);
    void createPasswordResetTokenForUser(User user, String token);
    String validatePasswordResetToken(String token);
    Optional<User> getUserByPasswordResetToken(String token);
    void changeUserPassword(User user, String password, String token);
    User updateUser2FA(boolean use2FA);
    String generateQRUrl(String email, String deviceId) throws WriterException, IOException;
    String normalLogin(String email, String deviceId, DeviceType deviceType);
    String remoteLogin(String username, String deviceId);
}
