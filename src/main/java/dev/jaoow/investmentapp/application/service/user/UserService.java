package dev.jaoow.investmentapp.application.service.user;

import dev.jaoow.investmentapp.application.dto.request.UserLoginRequest;
import dev.jaoow.investmentapp.application.dto.response.UserLoginResponse;
import dev.jaoow.investmentapp.application.dto.response.UserRegisterResponse;
import dev.jaoow.investmentapp.application.exception.EmailAlreadyInUseException;
import dev.jaoow.investmentapp.application.exception.ResourceNotFoundException;
import dev.jaoow.investmentapp.domain.entity.user.Role;
import dev.jaoow.investmentapp.domain.entity.user.User;
import dev.jaoow.investmentapp.domain.repository.user.RoleRepository;
import dev.jaoow.investmentapp.domain.repository.user.UserRepository;
import dev.jaoow.investmentapp.infrastructure.security.JwtService;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class UserService implements UserDetailsService {

    private final ModelMapper modelMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, ModelMapper modelMapper, AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
        for (Role role : user.getRoles()) {
            grantedAuthorities.add(new SimpleGrantedAuthority(role.getName()));
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), user.getPassword(), grantedAuthorities
        );
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private Role findRole(String roleName) {
        return roleRepository.findByName(roleName).orElseThrow(() -> new ResourceNotFoundException("Role not found with name" + roleName));
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

        return new UserLoginResponse(jwt);
    }

    public UserRegisterResponse register(UserLoginRequest userLoginRequest) {
        if (userRepository.existsByEmail(userLoginRequest.getEmail())) {
            throw new EmailAlreadyInUseException("Email already in use");
        }

        User user = new User();
        user.setEmail(userLoginRequest.getEmail());
        user.setPassword(passwordEncoder.encode(userLoginRequest.getPassword()));

        Role role = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        user.setRoles(new HashSet<>(Set.of(role)));

        user = userRepository.save(user);
        return modelMapper.map(user, UserRegisterResponse.class);
    }

    public boolean assignRoleToUser(String userEmail, String roleName) {
        User user = findUserByEmail(userEmail);
        Role role = findRole(roleName);

        user.getRoles().add(role);
        userRepository.save(user);
        return true;
    }

}
