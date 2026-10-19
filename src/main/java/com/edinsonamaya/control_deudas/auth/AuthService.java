package com.edinsonamaya.control_deudas.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.edinsonamaya.control_deudas.common.error.InvalidCredentialsException;
import com.edinsonamaya.control_deudas.common.error.ResourceAlreadyExistsException;
import com.edinsonamaya.control_deudas.security.JwtService;
import com.edinsonamaya.control_deudas.user.User;
import com.edinsonamaya.control_deudas.user.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResourceAlreadyExistsException("Usuario", "email", request.email());
        }
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .build();
        userRepository.save(user);
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getEmail());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException();
        }
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getEmail());
    }
}
