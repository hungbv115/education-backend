package com.education.model.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
public class Device {
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Getter
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DeviceType deviceType; // Enum: mobile, laptop

    @Getter
    @Column(nullable = false, unique = true)
    private String deviceId; // Unique device identifier

    @Column(nullable = false)
    private boolean isLoggedIn;

    @Getter
    @Column(nullable = false)
    private LocalDateTime lastLoginTime;

    // Getters and Setters


    public void setId(Long id) {
        this.id = id;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }
}

