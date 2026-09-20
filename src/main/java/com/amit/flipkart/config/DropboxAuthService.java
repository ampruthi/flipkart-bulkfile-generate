package com.amit.flipkart.config;

import com.amit.flipkart.config.DropboxProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class DropboxAuthService {

    private static final String TOKEN_URL =
            "https://api.dropbox.com/oauth2/token";

    private final DropboxProperties properties;
    private final RestClient restClient;

    public DropboxAuthService(DropboxProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().build();
    }

    public String getAccessToken() {

        String credentials =
                properties.getAppKey()
                        + ":"
                        + properties.getAppSecret();

        String basicAuth = Base64.getEncoder()
                .encodeToString(
                        credentials.getBytes(StandardCharsets.UTF_8)
                );

        MultiValueMap<String, String> form =
                new LinkedMultiValueMap<>();

        form.add("grant_type", "refresh_token");
        form.add(
                "refresh_token",
                properties.getRefreshToken()
        );

        DropboxTokenResponse response = restClient.post()
                .uri(TOKEN_URL)
                .contentType(
                        MediaType.APPLICATION_FORM_URLENCODED
                )
                .header(
                        "Authorization",
                        "Basic " + basicAuth
                )
                .body(form)
                .retrieve()
                .body(DropboxTokenResponse.class);

        if (response == null ||
                response.access_token() == null ||
                response.access_token().isBlank()) {

            throw new IllegalStateException(
                    "Dropbox did not return an access token"
            );
        }

        return response.access_token();
    }

    public record DropboxTokenResponse(
            String access_token,
            String token_type,
            Long expires_in,
            String refresh_token,
            String scope
    ) {
    }
}