package com.pngthanh.cineverse.media.config;

import com.pngthanh.cineverse.media.service.MediaStorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MediaWebConfig implements WebMvcConfigurer {
    private final MediaStorageService storage;

    public MediaWebConfig(MediaStorageService storage) {
        this.storage = storage;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = storage.uploadDirectory().toUri().toString();
        registry.addResourceHandler("/uploads/movies/**").addResourceLocations(location);
    }
}
