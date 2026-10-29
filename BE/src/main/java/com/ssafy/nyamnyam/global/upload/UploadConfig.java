package com.ssafy.nyamnyam.global.upload;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * 업로드한 이미지를 /api/v1/uploads/images/** 로 내보낸다.
 * Nginx가 /api/ 만 백엔드로 넘기므로 Nginx 설정을 바꾸지 않아도 된다. 파일 이름이 UUID라 내용이 바뀌지 않아 오래 캐시한다.
 */
@Configuration
@EnableConfigurationProperties(UploadProperties.class)
public class UploadConfig implements WebMvcConfigurer {

    private final UploadProperties properties;

    public UploadConfig(UploadProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(properties.dir()).toAbsolutePath().normalize().resolve("images").toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler(UploadProperties.IMAGE_PATH + "**")
                .addResourceLocations(location)
                .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic());
    }
}
