package com.ai.manager.system.config;

import com.ai.manager.system.service.impl.EcImageUploadServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class EcUploadResourceConfig implements WebMvcConfigurer {

    private final EcImageUploadServiceImpl ecImageUploadService;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = ecImageUploadService.getUploadDir().toUri().toString();
        registry.addResourceHandler("/uploads/ecommerce/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
