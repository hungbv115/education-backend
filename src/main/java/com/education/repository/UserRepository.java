package com.education.repository;

import com.education.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "INSERT INTO `order`.users(username, `role`, password) VALUES(?1, 'USER', ?2)", nativeQuery = true)
    void insertNewUser(String username, String password);
}