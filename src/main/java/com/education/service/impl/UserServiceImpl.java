package com.education.service.impl;

import com.education.error.UserAlreadyExistException;
import com.education.model.dto.UserDto;
import com.education.model.entity.*;
import com.education.repository.*;
import com.education.security.HandleJWT;
import com.education.service.UserService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.maxmind.geoip2.DatabaseReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserLocationRepository userLocationRepository;
    private final RoleRepository roleRepository;
    private final Environment env;
    private final VerificationTokenRepository tokenRepository;

    @Autowired
    @Qualifier("GeoIPCountry")
    private DatabaseReader databaseReader;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    private final HandleJWT handleJWT;

    private final AuthenticationManager authenticationManager;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, UserLocationRepository userLocationRepository, RoleRepository roleRepository, Environment env, VerificationTokenRepository tokenRepository, HandleJWT handleJWT, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userLocationRepository = userLocationRepository;
        this.roleRepository = roleRepository;
        this.env = env;
        this.tokenRepository = tokenRepository;
        this.handleJWT = handleJWT;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public String normalLogin(String email, String deviceId, DeviceType deviceType) {
        // Xác thực username và password
        User user = userRepository.findByEmail(email);

        // Kiểm tra trạng thái của thiết bị
        Optional<Device> existingDevice = deviceRepository.findByUserIdAndDeviceId(user.getId(), deviceId);
        if (existingDevice.isPresent()) {
            if (existingDevice.get().isLoggedIn()) {
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                user.getEmail(),
                                user.getPassword()
                        )
                );
                return handleJWT.generateToken(user.getEmail());
            } else {
                return "Device is waiting for remote login approval.";
            }
        }

        // Thêm thiết bị mới vào trạng thái "chờ"
        Device newDevice = new Device();
        newDevice.setUser(user);
        newDevice.setDeviceId(deviceId);
        newDevice.setDeviceType(deviceType);
        newDevice.setLoggedIn(false);
        newDevice.setLastLoginTime(java.time.LocalDateTime.now());
        deviceRepository.save(newDevice);

        return "Device is waiting for remote login approval.";
    }

    @Transactional
    public String remoteLogin(String username, String deviceId) {
        User user = userRepository.findByEmail(username);

        Device device = deviceRepository.findByUserIdAndDeviceId(user.getId(), deviceId)
                .orElseThrow(() -> new RuntimeException("No pending device found"));

        device.setLoggedIn(true);
        deviceRepository.save(device);

        return "Device approved for login.";
    }

    @Override
    @Transactional
    public User signup(UserDto accountDto) {
        if (emailExists(accountDto.getEmail())) {
            throw new UserAlreadyExistException("There is an account with that email address: " + accountDto.getEmail());
        }
        final User user = new User();

        user.setFirstName(accountDto.getFirstName());
        user.setLastName(accountDto.getLastName());
        user.setPassword(passwordEncoder.encode(accountDto.getPassword()));
        user.setEmail(accountDto.getEmail());
        user.setUsing2FA(accountDto.isUsing2FA());
        user.setRoles(Collections.singletonList(roleRepository.findByName("ROLE_USER")));
        return userRepository.save(user);
    }

    @Override
    public void addUserLocation(User user, String ip) {

        if(!isGeoIpLibEnabled()) {
            return;
        }

        try {
            final InetAddress ipAddress = InetAddress.getByName(ip);
            final String country = databaseReader.country(ipAddress)
                    .getCountry()
                    .getName();
            UserLocation loc = new UserLocation(country, user);
            loc.setEnabled(true);
            userLocationRepository.save(loc);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void createVerificationTokenForUser(final User user, final String token) {
        final VerificationToken myToken = new VerificationToken(token, user);
        tokenRepository.save(myToken);
    }

    @Override
    public VerificationToken generateNewVerificationToken(final String existingVerificationToken) {
        VerificationToken vToken = tokenRepository.findByToken(existingVerificationToken);
        vToken.updateToken(UUID.randomUUID()
                .toString());
        vToken = tokenRepository.save(vToken);
        return vToken;
    }

    @Override
    public User getUser(final String verificationToken) {
        final VerificationToken token = tokenRepository.findByToken(verificationToken);
        if (token != null) {
            return token.getUser();
        }
        return null;
    }

    @Override
    public VerificationToken getVerificationToken(final String VerificationToken) {
        return tokenRepository.findByToken(VerificationToken);
    }

    @Override
    public void saveRegisteredUser(final User user) {
        userRepository.save(user);
    }

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public void createPasswordResetTokenForUser(User user, String token) {
        PasswordResetToken resetToken = new PasswordResetToken(token, user);
        passwordResetTokenRepository.save(resetToken);
    }

    @Override
    public String validatePasswordResetToken(String token) {
        final PasswordResetToken passToken = passwordResetTokenRepository.findByToken(token);

        return !isTokenFound(passToken) ? "invalidToken"
                : isTokenExpired(passToken) ? "expired"
                : null;
    }

    @Override
    public Optional<User> getUserByPasswordResetToken(String token) {
        return Optional.ofNullable(passwordResetTokenRepository.findByToken(token) .getUser());
    }

    @Override
    public void changeUserPassword(final User user, final String password, String token) {
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);

        passwordResetTokenRepository.deleteTokenAfterResetSuccess(token);
    }

    @Override
    public User updateUser2FA(boolean use2FA) {
        final Authentication curAuth = SecurityContextHolder.getContext()
                .getAuthentication();
        User currentUser = (User) curAuth.getPrincipal();
        currentUser.setUsing2FA(use2FA);
        currentUser = userRepository.save(currentUser);
        final Authentication auth = new UsernamePasswordAuthenticationToken(currentUser, currentUser.getPassword(), curAuth.getAuthorities());
        SecurityContextHolder.getContext()
                .setAuthentication(auth);
        return currentUser;
    }

    @Override
    public String generateQRUrl(String email, String deviceId) throws WriterException, IOException {
        String data = "http://localhost:8080/api/remote-login?username=" + email + "&targetDeviceId=" + deviceId;
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix matrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 200, 200);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
        byte[] pngByteArray = outputStream.toByteArray();
        return Arrays.toString(pngByteArray);
    }

    private boolean emailExists(final String email) {
        return userRepository.findByEmail(email) != null;
    }

    private boolean isGeoIpLibEnabled() {
        return Boolean.parseBoolean(env.getProperty("geo.ip.lib.enabled"));
    }

    private boolean isTokenFound(PasswordResetToken passToken) {
        return passToken != null;
    }

    private boolean isTokenExpired(PasswordResetToken passToken) {
        final Calendar cal = Calendar.getInstance();
        return passToken.getExpiryDate().before(cal.getTime());
    }

}
