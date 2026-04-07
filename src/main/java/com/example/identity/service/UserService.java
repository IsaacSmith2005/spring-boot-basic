package com.example.identity.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.identity.dto.request.UserCreationRequest;
import com.example.identity.dto.request.UserUpdateRequest;
import com.example.identity.dto.response.ProfileResponse;
import com.example.identity.entity.User;
import com.example.identity.enums.Role;
import com.example.identity.exception.CustomException;
import com.example.identity.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService {

    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    

    public User createUser(UserCreationRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new CustomException("Username already exists", "username");
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        HashSet<String> roles = new HashSet<>();
        roles.add(Role.USER.name());

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .birthYear(request.getBirthYear())
                .gender(request.getGender())
                .password(request.getPassword())
                .roles(roles)
                .build();   

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        return userRepository.save(user);
    } 

    @PreAuthorize("hasRole('ADMIN')")
    public User updateUser(String id, UserUpdateRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if(request.getBirthYear() != null) {
            user.setBirthYear(request.getBirthYear());
        }
        if(request.getGender() != null) {
            user.setGender(request.getGender());
        }
        return userRepository.save(user);
    }

    @PreAuthorize("isAuthenticated()")
    public ProfileResponse updateMyProfile(UserUpdateRequest request) {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(name).orElseThrow(
            () -> new RuntimeException("User not found")
        );
        
        if(request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if(request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if(request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if(request.getBirthYear() != null) {
            user.setBirthYear(request.getBirthYear());
        }
        if(request.getGender().equals("Nam") || request.getGender().equals("nam") || request.getGender().equals("Nữ") || request.getGender().equals("nữ")) {
            user.setGender(request.getGender());
        } else {
            throw new RuntimeException("Giới tính phải là nam hoặc nữ");
        }
        

        int currentYear = java.time.Year.now().getValue();
        if (user.getBirthYear() > currentYear) {
            throw new RuntimeException("Birth year cannot be in the future");
        }
        int age = currentYear - user.getBirthYear();
        String ageGroup = age < 18 ? "Trẻ em" : "Người lớn";

        return ProfileResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .birthYear(user.getBirthYear())
            .age(age)
            .ageGroup(ageGroup)
            .gender(user.getGender())
            .build();
    }


    @PreAuthorize("isAuthenticated()")
    public ProfileResponse getMyProfile() {
        String name  = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(name).orElseThrow(
            () -> new RuntimeException("User not found")
        );

        int currentYear = java.time.Year.now().getValue();
        if (user.getBirthYear() > currentYear) {
            throw new RuntimeException("Birth year cannot be in the future");
        }
        int age = currentYear - user.getBirthYear();
        String ageGroup = age < 18 ? "Trẻ em" : "Người lớn";

        return ProfileResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .birthYear(user.getBirthYear())
            .age(age)
            .ageGroup(ageGroup)
            .gender(user.getGender())
            .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<ProfileResponse> getUserAll() {
        log.info("Getting all users");
        List<User> users = userRepository.findAll();
        List<ProfileResponse> responses = new ArrayList<>();

        int currentYear = java.time.Year.now().getValue();
        for(User user : users) {
            Integer birthYear = user.getBirthYear();
            Integer age = null;
            String ageGroup = "Không xác định";

            if (birthYear != null) {
                age = currentYear - birthYear;
                ageGroup = age < 18 ? "Trẻ em" : "Người lớn";
            }

            if((age != null && age < 18 && user.getGender().equals("Nữ")) || (age != null && age > 18 && user.getGender().equals("Nam"))) {
                responses.add(ProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .birthYear(birthYear)
                .age(age)
                .ageGroup(ageGroup)
                .gender(user.getGender())
                .build()
            );
            }
        }
        return responses;
    }

    @PostAuthorize("hasRole('ADMIN') or returnObject.username == authentication.username")
    public User getUserById(String id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(String id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(id);
    }
}
