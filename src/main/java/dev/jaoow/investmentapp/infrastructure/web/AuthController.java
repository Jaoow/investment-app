package dev.jaoow.investmentapp.infrastructure.web;

import dev.jaoow.investmentapp.application.dto.request.UserLoginRequest;
import dev.jaoow.investmentapp.application.dto.request.UserRegisterRequest;
import dev.jaoow.investmentapp.application.dto.response.UserLoginResponse;
import dev.jaoow.investmentapp.application.dto.response.UserResponse;
import dev.jaoow.investmentapp.application.service.user.AuthService;
import dev.jaoow.investmentapp.application.service.user.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    public AuthController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping("/login")
    public UserLoginResponse login(@RequestBody UserLoginRequest userLoginRequest) {
        return authService.login(userLoginRequest);
    }

    @GetMapping("/me")
    public UserResponse me(Principal principal) {
        return userService.self(principal);
    }

    @PostMapping("/register")
    public UserResponse register(@RequestBody UserRegisterRequest userRegisterRequest) {
        return userService.register(userRegisterRequest);
    }

    @PostMapping("/assign-role")
    public void assignRole(@RequestParam String email, @RequestParam String role) {
        userService.assignRoleToUser(email, role);
    }
}
