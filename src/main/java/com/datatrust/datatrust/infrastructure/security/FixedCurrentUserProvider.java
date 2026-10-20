package com.datatrust.datatrust.infrastructure.security;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.datatrust.datatrust.application.ports.CurrentUserProvider;

// Temporal: se sustituye por la autenticación real en la entrega 3
@Component
public class FixedCurrentUserProvider implements CurrentUserProvider {

    private static final UUID DEV_USER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Override
    public UUID currentUserId() {
        return DEV_USER;
    }
}