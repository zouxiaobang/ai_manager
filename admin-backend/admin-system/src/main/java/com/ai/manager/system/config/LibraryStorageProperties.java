package com.ai.manager.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "library.storage")
public class LibraryStorageProperties {

    private String localRoot = "./uploads/library";
}
