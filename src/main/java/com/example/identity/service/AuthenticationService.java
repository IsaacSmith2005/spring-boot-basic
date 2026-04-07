package com.example.identity.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.example.identity.dto.request.LogoutRequest;
import com.example.identity.dto.request.AuthenticationRequest;
import com.example.identity.dto.request.IntrospectRequest;
import com.example.identity.dto.response.AuthenticationResponse;
import com.example.identity.dto.response.IntrospectResponse;
import com.example.identity.entity.InvalidatedToken;
import com.example.identity.entity.User;
import com.example.identity.exception.AuthenticateException;
import com.example.identity.repository.InvalidatedTokenRepository;
import com.example.identity.repository.UserRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

import static lombok.AccessLevel.PRIVATE;

import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class AuthenticationService {
    UserRepository userRepository;
    InvalidatedTokenRepository invalidatedTokenRepository;

    @NonFinal // Cho phép thay đổi giá trị trong runtime
    @Value("${jwt.secret}") // Đọc key từ file application.properties
    protected String SIGNER_KEY; // Key để sign token

    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken(); // Token cần verify
        boolean isValid = true;
    
        try {
            verifyToken(token);
        } catch (RuntimeException e) {
            isValid = false;
        }
        
        // Trả về response
        return IntrospectResponse.builder()
            .valid(isValid)
            .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if(!authenticated) {
            throw new AuthenticateException("Invalid username or password");
        }

        var token = generateToken(user);
        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }

    private String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256); // Thuật toán sign

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder() // Claims của token
                .subject(user.getUsername()) // Người dùng
                .issuer("Haya") // Issuer
                .issueTime(new Date(0)) // Thời gian tạo
                .expirationTime(new Date(
                    Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli()
                )) // Thời gian hết hạn
                .jwtID(UUID.randomUUID().toString()) // ID của token
                .claim("scope", buildScope(user)) // Scope
        .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject()); // Payload của token

        JWSObject jwsObject = new JWSObject(header, payload); // Tạo token

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes())); // Sign token
            return jwsObject.serialize(); // Trả về token
        } catch (JOSEException e) { // Bắt exception
            System.out.println("Error signing token: " + e.getMessage()); // Log error
            throw new RuntimeException(e);
        }
    }

    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        if (!CollectionUtils.isEmpty(user.getRoles()))
            user.getRoles().forEach(stringJoiner::add);
            System.out.println("Scope: " + stringJoiner.toString());
        return stringJoiner.toString();
    }

    public void logout(LogoutRequest request) throws JOSEException, ParseException {
        // Thực hiện login đăng xuất
        var signToken = verifyToken(request.getToken());

        String jit = signToken.getJWTClaimsSet().getJWTID();
        Date expityTime = signToken.getJWTClaimsSet().getExpirationTime();
        
        invalidatedTokenRepository.save(InvalidatedToken.builder()
            .id(jit)
            .expiryTime(expityTime)
            .build());  
    }

    private SignedJWT verifyToken(String token) throws JOSEException, ParseException {
        // Xác thực token       
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());
        
        // Parse token
        SignedJWT signedJWT = SignedJWT.parse(token);

        // Kiểm tra token hết hạn hay chưa
        Date expityTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        var verified = signedJWT.verify(verifier);

        // Kiểm tra token có hợp lệ và chưa hết hạn
        if (!verified || expityTime.before(new Date())) {
            throw new RuntimeException("Token expired or invalid");
        }

        // Kiểm tra token đã được invalidate chưa
        if (invalidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID())) {
            throw new RuntimeException("Token already invalidated");
        }
        
        return signedJWT;
    }
}

