package com.example.identity.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.example.identity.dto.request.LogoutRequest;
import com.example.identity.dto.request.RefreshRequest;
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
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class AuthenticationService {
    UserRepository userRepository;
    InvalidatedTokenRepository invalidatedTokenRepository;

    @NonFinal // Cho phép thay đổi giá trị trong runtime
    @Value("${jwt.secret}") // Đọc key từ file application.properties
    protected String SIGNER_KEY; // Key để sign token

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refresh-duration}")
    protected long REFRESH_DURATION;

    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken(); // Token cần verify
        boolean isValid = true;
    
        try {
            verifyToken(token, false);
        } catch (RuntimeException e) {
            isValid = false;
        }
        
        // Trả về response
            return IntrospectResponse.builder()
            .valid(isValid)
            .build();
    }

    // Xác thực người dùng
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // So sánh password
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if(!authenticated) {
            throw new AuthenticateException("Invalid username or password");
        }

        // Tạo token
        var token = generateToken(user);
        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }

    // Tạo token
    private String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256); // Thuật toán sign

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder() // Claims của token

                .subject(user.getUsername()) // Người dùng
                .issuer("Haya") // Issuer: định danh thực thể hoặc nhà phát hành token
                .issueTime(new Date(0)) // Thời gian tạo
                .expirationTime(new Date(
                    Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()
                )) // Thời gian hết hạn
                .jwtID(UUID.randomUUID().toString()) // ID của token
                .claim("scope", buildScope(user)) // Scope: quyền hạn của user
                .claim("user_id", user.getId())
                .claim("user_email", user.getEmail())
        .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject()); // Payload của token

        JWSObject jwsObject = new JWSObject(header, payload); // Tạo token

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes())); // Sign token: thêm chữ ký vào token
            return jwsObject.serialize(); // Trả về token
        } catch (JOSEException e) { // Bắt exception
            System.out.println("Error signing token: " + e.getMessage()); // Log error
            throw new RuntimeException(e);
        }
    }

    // Tạo scope
    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        if (!CollectionUtils.isEmpty(user.getRoles())) { // Nếu user có role
            user.getRoles().forEach(stringJoiner::add); // Thêm role vào scope
            System.out.println("Scope: " + stringJoiner.toString()); // Log scope
        }
        return stringJoiner.toString();
    }

    // Đăng xuất    
    public void logout(LogoutRequest request) throws JOSEException, ParseException {
        try { 
            // Thực hiện login đăng xuất
            var signToken = verifyToken(request.getToken(), false);

            String jit = signToken.getJWTClaimsSet().getJWTID(); // ID của token
            Date expityTime = signToken.getJWTClaimsSet().getExpirationTime(); // Thời gian hết hạn
        
            // Lưu token vào database
            invalidatedTokenRepository.save(InvalidatedToken.builder()
                .id(jit)
                .expiryTime(expityTime)
                .build());  
        } catch (Exception e) {
            log.info("Token is expired or invalid: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public AuthenticationResponse refreshToken(RefreshRequest request) throws JOSEException, ParseException {
        var signedJWT = verifyToken(request.getToken(), true);
        var jit = signedJWT.getJWTClaimsSet().getJWTID();
        var expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        
        // Lưu token vào database
        invalidatedTokenRepository.save(InvalidatedToken.builder()
            .id(jit)
            .expiryTime(expiryTime)
            .build());

        var username = signedJWT.getJWTClaimsSet().getSubject();
        
        var user = userRepository.findByUsername(username).orElseThrow(
            () -> new RuntimeException("User not found") 
        );

        var token = generateToken(user);
        return AuthenticationResponse.builder()
            .token(token)
            .authenticated(true)
            .build();
    }

    private SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        // Xác thực token       
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());  
        
        // Parse token
        SignedJWT signedJWT = SignedJWT.parse(token);

        // Kiểm tra token hết hạn hay chưa
        Date expityTime = (isRefresh) 
            // Nếu là refresh token thì cộng thêm thời gian refresh
            ? new Date (signedJWT.getJWTClaimsSet().getExpirationTime()
                .toInstant().plus(REFRESH_DURATION, ChronoUnit.SECONDS).toEpochMilli())
            // Nếu là access token thì lấy thời gian hết hạn của token
            : signedJWT.getJWTClaimsSet().getExpirationTime();

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

