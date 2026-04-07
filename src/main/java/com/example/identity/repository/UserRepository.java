package com.example.identity.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.identity.dto.request.UserCreationRequest;
import com.example.identity.dto.request.UserUpdateRequest;
import com.example.identity.entity.User;

public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByUsername(String id);
    boolean existsByEmail(String email);
    Optional<User> findByUsername(String username);

    User save(UserCreationRequest user);

    User save(UserUpdateRequest user);

    boolean existsByBirthYearAndUsernameNot(Integer birthYear, String username);
}
