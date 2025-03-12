package ru.shortcut.app;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.shortcut.client.ExternalApiClient;
import ru.shortcut.properties.ServiceProperties;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class MyService {

    private final ExternalApiClient externalApiClient;
    private final ServiceProperties serviceProperties;

    /**
     * Метод принимает один параметр и в зависимости от настройки выполняет логику:
     * - Если asyncEnabled = true, то возвращается CompletableFuture, созданный с помощью supplyAsync.
     * - Иначе данные обрабатываются синхронно и результат оборачивается в уже завершённый CompletableFuture.
     */
    public CompletableFuture<String> execute(String input) {
        if (Boolean.TRUE.equals(serviceProperties.getAsyncEnabled())) {
            // Асинхронное выполнение
            return CompletableFuture.supplyAsync(() -> {
                String data = externalApiClient.getData(input);
                return "Async Processed: " + data;
            });
        } else {
            // Синхронное выполнение
            String data = externalApiClient.getData(input);
            return CompletableFuture.completedFuture("Sync Processed: " + data);
        }
    }
}
