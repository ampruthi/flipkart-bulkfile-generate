package com.amit.flipkart.config;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DropboxProperties.class)
public class DropboxConfig {

    @Bean
    public DbxClientV2 dropboxClient(
            DropboxProperties properties,
            DropboxAuthService authService) {

        validate(properties);

        String accessToken = authService.getAccessToken();

        DbxRequestConfig config =
                DbxRequestConfig.newBuilder(
                        "flipkart-uploader-poc"
                ).build();

        return new DbxClientV2(
                config,
                accessToken
        );
    }

    private void validate(DropboxProperties properties) {

        if (isBlank(properties.getAppKey())) {
            throw new IllegalStateException(
                    "DROPBOX_APP_KEY is not configured");
        }

        if (isBlank(properties.getAppSecret())) {
            throw new IllegalStateException(
                    "DROPBOX_APP_SECRET is not configured");
        }

        if (isBlank(properties.getRefreshToken())) {
            throw new IllegalStateException(
                    "DROPBOX_REFRESH_TOKEN is not configured");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}