package ru.shortcut.client;

import org.springframework.stereotype.Service;

@Service
public class ExternalApiClientImpl implements ExternalApiClient {
    @Override
    public String getData(String param) {
        return param;
    }
}
