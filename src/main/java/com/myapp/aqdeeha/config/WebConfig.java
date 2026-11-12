package com.myapp.aqdeeha.config;

import com.myapp.aqdeeha.common.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves ONLY the {@code public/} storage folder (profile and vehicle photos) at {@code /files/public/**}.
 * The {@code private/} folder (encrypted ID cards) is never exposed.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final FileStorageService storage;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = storage.root().resolve(FileStorageService.PUBLIC_DIR).toUri().toString();
        registry.addResourceHandler("/files/public/**").addResourceLocations(location);
    }
}
