package com.datatrust.datatrust.application.ports;

import java.util.UUID;

public interface CurrentUserProvider {
    UUID currentUserId();
}