package com.example.identity.config;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {"/api/users", "/auth/login", "/auth/introspect", "/auth/logout", "/auth/refresh"}; // endpoints public

    @Value("${jwt.secret}")
    private String signerKey;

    @Autowired
    private CustomJwtDecoder customJwtDecoder; // decoder for jwt

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
            .authorizeHttpRequests(request -> 
                request.requestMatchers(HttpMethod.POST, PUBLIC_ENDPOINTS).permitAll()
                .anyRequest().authenticated()); // tất cả các endpoint khác đều yêu cầu authentication

        // cấu hình resource server cho oauth2
        httpSecurity.oauth2ResourceServer(oauth2 ->  
            oauth2.jwt(jwtConfigurer ->
                jwtConfigurer.decoder(customJwtDecoder) // decoder for jwt
                .jwtAuthenticationConverter(jwtAuthenticationConverter()) // converter for jwt
            )
        );  

        httpSecurity.csrf(AbstractHttpConfigurer::disable); // tắt csrf cho oauth2

        return httpSecurity.build();
    }

    // converter for jwt
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter(); // converter for jwt
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("ROLE_"); // prefix for role
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter(); // converter for jwt
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter); // set converter for jwt
        return jwtAuthenticationConverter;
    }

    // bỏ qua các endpoint công khai
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(HttpMethod.POST, PUBLIC_ENDPOINTS);
    }

    // decoder cho jwt
    @Bean
    JwtDecoder jwtDecoder() {
        SecretKeySpec secretKeySpec = new SecretKeySpec(signerKey.getBytes(), "HS256");
        return NimbusJwtDecoder
                .withSecretKey(secretKeySpec)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    // password encoder
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}