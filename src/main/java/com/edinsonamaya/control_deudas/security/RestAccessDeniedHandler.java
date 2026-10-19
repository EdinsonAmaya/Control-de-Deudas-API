package com.edinsonamaya.control_deudas.security;

import tools.jackson.databind.ObjectMapper;
import com.edinsonamaya.control_deudas.common.error.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Devuelve un 403 con el JSON estandar cuando el usuario autenticado
 * intenta acceder a recursos que no le pertenecen.
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpServletResponse.SC_FORBIDDEN,
                "Forbidden",
                "No tiene permiso para acceder a este recurso.",
                request.getRequestURI());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
