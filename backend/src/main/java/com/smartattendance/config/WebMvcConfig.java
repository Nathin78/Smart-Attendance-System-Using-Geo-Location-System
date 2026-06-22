package com.smartattendance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final Path avatarDirectory;

    public WebMvcConfig(@Value("${app.upload.avatar-dir:backend/uploads/avatars}") String avatarDir) {
        this.avatarDirectory = Path.of(avatarDir).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(avatarDirectory.getParent().toUri().toString());
    }
}
