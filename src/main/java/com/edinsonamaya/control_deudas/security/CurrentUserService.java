package com.edinsonamaya.control_deudas.security;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.edinsonamaya.control_deudas.common.error.ResourceNotFoundException;
import com.edinsonamaya.control_deudas.user.User;
import com.edinsonamaya.control_deudas.user.UserRepository;

/**
 * Resuelve el usuario autenticado a partir del token JWT de la peticion actual.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User requireUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            Jwt jwt = jwtAuthentication.getToken();
            return userRepository.findByEmail(jwt.getSubject())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario", jwt.getSubject()));
        }
        throw new AccessDeniedException("Se requiere autenticación para acceder a este recurso.");
    }
}
