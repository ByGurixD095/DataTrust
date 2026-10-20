package com.datatrust.application.ports;

import java.util.UUID;

public interface CurrentUserProvider {
    UUID currentUserId();
}