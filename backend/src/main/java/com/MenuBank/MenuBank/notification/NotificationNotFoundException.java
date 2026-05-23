package com.MenuBank.MenuBank.notification;

import java.util.UUID;

public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(UUID id) {
        super("Notificação não encontrada: " + id);
    }
}
