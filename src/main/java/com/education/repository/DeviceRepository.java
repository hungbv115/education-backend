package com.education.repository;

import com.education.model.entity.Device;
import com.education.model.entity.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByUserIdAndDeviceId(Long userId, String deviceId);
}
