package com.education.service.impl;

import com.education.error.UserAlreadyExistException;
import com.education.model.dto.UserDto;
import com.education.model.entity.User;
import com.education.model.entity.UserLocation;
import com.education.model.entity.VerificationToken;
import com.education.repository.RoleRepository;
import com.education.repository.UserLocationRepository;
import com.education.repository.UserRepository;
import com.education.repository.VerificationTokenRepository;
import com.education.service.UserService;
import com.maxmind.geoip2.DatabaseReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.util.Collections;
import java.util.UUID;

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

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, UserLocationRepository userLocationRepository, RoleRepository roleRepository, Environment env, VerificationTokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userLocationRepository = userLocationRepository;
        this.roleRepository = roleRepository;
        this.env = env;
        this.tokenRepository = tokenRepository;
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

    private boolean emailExists(final String email) {
        return userRepository.findByEmail(email) != null;
    }

    private boolean isGeoIpLibEnabled() {
        return Boolean.parseBoolean(env.getProperty("geo.ip.lib.enabled"));
    }

}