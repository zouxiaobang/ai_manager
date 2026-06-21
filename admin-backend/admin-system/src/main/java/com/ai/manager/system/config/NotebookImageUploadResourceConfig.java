package com.ai.manager.system.config;

import com.ai.manager.system.service.impl.NbNoteImageUploadServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class NotebookImageUploadResourceConfig implements WebMvcConfigurer {

    private final NbNoteImageUploadServiceImpl nbNoteImageUploadService;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = nbNoteImageUploadService.getUploadDir().toUri().toString();
        registry.addResourceHandler("/uploads/notebook/images/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
