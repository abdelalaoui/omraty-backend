package com.omraty.backend.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String uploadDir;
    private final String baseUrl;

    public WebConfig(
            @Value("${app.upload.dir}") String uploadDir,
            @Value("${app.upload.base-url}") String baseUrl) {
        this.uploadDir = uploadDir;
        this.baseUrl = baseUrl;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + Path.of(uploadDir).toAbsolutePath().normalize() + "/";
        registry.addResourceHandler(baseUrl + "/**").addResourceLocations(location);
    }
}
