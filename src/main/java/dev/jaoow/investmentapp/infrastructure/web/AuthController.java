package dev.jaoow.investmentapp.infrastructure.web;

import dev.jaoow.investmentapp.application.dto.request.UserLoginRequest;
import dev.jaoow.investmentapp.application.dto.response.UserLoginResponse;
import dev.jaoow.investmentapp.application.dto.response.UserRegisterResponse;
import dev.jaoow.investmentapp.application.service.user.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public UserLoginResponse login(@RequestBody UserLoginRequest userLoginRequest) {
        return userService.login(userLoginRequest);
    }

    @PostMapping("/register")
    public UserRegisterResponse register(@RequestBody UserLoginRequest userLoginRequest) {
        return userService.register(userLoginRequest);
    }
}
