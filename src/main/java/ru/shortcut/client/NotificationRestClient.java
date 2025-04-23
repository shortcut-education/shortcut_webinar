package ru.shortcut.client;

import org.springframework.stereotype.Component;

@Component
public interface NotificationRestClient {
    void notify(Payment payment);
}
