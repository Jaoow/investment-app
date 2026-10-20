package dev.jaoow.investmentapp.application.service.user;

import dev.jaoow.investmentapp.application.dto.request.UserLoginRequest;
import dev.jaoow.investmentapp.application.dto.response.UserLoginResponse;
import dev.jaoow.investmentapp.infrastructure.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {

    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    public AuthService(JwtService jwtService, AuthenticationManager authenticationManager, UserService userService) {
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userService = userService;
    }

    public UserLoginResponse login(UserLoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(), loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetails principal = (UserDetails) authentication.getPrincipal();
        String jwt = jwtService.generateToken(principal);
        return new UserLoginResponse(jwt, jwtService.generateRefreshToken(principal));
    }

    public UserLoginResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank() || !jwtService.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String email = jwtService.extractUsername(refreshToken);
        UserDetails principal = userService.loadUserByUsername(email);
        if (!jwtService.isRefreshTokenValid(refreshToken, principal)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        return new UserLoginResponse(jwtService.generateToken(principal), jwtService.generateRefreshToken(principal));
    }
}
