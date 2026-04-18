package com.example.identity.repository;

import com.example.identity.entity.InvalidatedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Chức năng: Lưu trữ các token đã bị hủy
@Repository
public interface InvalidatedTokenRepository extends JpaRepository<InvalidatedToken, String> {
    
}
