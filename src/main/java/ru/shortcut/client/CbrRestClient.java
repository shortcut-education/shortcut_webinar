package ru.shortcut.client;

import org.springframework.stereotype.Component;

@Component
public interface CbrRestClient {
    CbrResponse dorequest();
}
