package dev.jaoow.investmentapp.application.service.user;

import dev.jaoow.investmentapp.application.dto.request.UserLoginRequest;
import dev.jaoow.investmentapp.application.dto.response.UserLoginResponse;
import dev.jaoow.investmentapp.infrastructure.security.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthService {

    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(JwtService jwtService, AuthenticationManager authenticationManager) {
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public UserLoginResponse login(UserLoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(), loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetails principal = (UserDetails) authentication.getPrincipal();
        String jwt;

        try {
            jwt = jwtService.generateToken(principal);
        } catch (Exception e) {
            log.error("Error while generating token", e);
            return null;
        }

        return new UserLoginResponse(jwt);
    }
}
