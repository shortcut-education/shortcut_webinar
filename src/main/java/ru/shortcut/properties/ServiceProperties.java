package ru.shortcut.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "service")
public class ServiceProperties {

    private Boolean asyncEnabled;
}
