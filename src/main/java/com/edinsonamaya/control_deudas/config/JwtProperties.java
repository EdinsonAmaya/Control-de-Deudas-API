package com.edinsonamaya.control_deudas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(String secret, String issuer, Duration expiration) {
}
