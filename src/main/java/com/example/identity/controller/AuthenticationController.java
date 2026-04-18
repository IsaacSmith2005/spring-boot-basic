package com.example.identity.controller;

import java.text.ParseException;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.identity.dto.response.APIResponse;
import com.example.identity.dto.response.AuthenticationResponse;
import com.example.identity.dto.response.IntrospectResponse;
import com.example.identity.dto.request.AuthenticationRequest;
import com.example.identity.dto.request.IntrospectRequest;
import com.example.identity.dto.request.LogoutRequest;
import com.example.identity.dto.request.RefreshRequest;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import com.example.identity.service.AuthenticationService;
import com.nimbusds.jose.JOSEException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {

    AuthenticationService authenticationService;
    
    @PostMapping("/login")
    APIResponse<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        var result = authenticationService.authenticate(request);
        return APIResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    @PostMapping("/introspect")
    APIResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request) throws ParseException, JOSEException {
        var result = authenticationService.introspect(request);
        return APIResponse.<IntrospectResponse>builder()
                .result(result)
                .build();
    }

    @PostMapping("/logout") 
    APIResponse<String> logout(@RequestBody LogoutRequest request) throws ParseException, JOSEException {
        authenticationService.logout(request);
        return APIResponse.<String>builder()
                .result("Đã logout thành công")
                .build();
    }

    @PostMapping("/refresh")
    APIResponse<AuthenticationResponse> refreshToken(@RequestBody RefreshRequest request) throws JOSEException, ParseException {
        var result = authenticationService.refreshToken(request);
        return APIResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }
}
