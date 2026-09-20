package com.amit.flipkart.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@Configuration
public class GoogleDriveConfig {

   // @Bean
    public Drive googleDrive(
            @Value("${app.google-drive.credentials}") Resource credentials,
            @Value("${app.google-drive.tokens-directory:./data/google-tokens}") String tokenDir)
            throws Exception {

        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        final GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        GoogleClientSecrets clientSecrets;
        try (InputStream in = credentials.getInputStream()) {
            clientSecrets = GoogleClientSecrets.load(
                    jsonFactory, new InputStreamReader(in));
        }

        Path tokenPath = Path.of(tokenDir);
        Files.createDirectories(tokenPath);

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                jsonFactory,
                clientSecrets,
                List.of(DriveScopes.DRIVE_FILE))
                .setDataStoreFactory(new FileDataStoreFactory(tokenPath.toFile()))
                .setAccessType("offline")
                .build();

        Credential credential = new AuthorizationCodeInstalledApp(
                flow,
                new LocalServerReceiver.Builder().setPort(8888).build())
                .authorize("flipkart-uploader");

        return new Drive.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("Flipkart Uploader POC")
                .build();
    }

    @Bean
    public Drive googleDrive(@Value("${app.google-drive.credentials}") Resource credentials) throws Exception {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        final GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        // Service Account authenticates automatically without opening a browser
        GoogleCredentials googleCredentials =
                com.google.auth.oauth2.GoogleCredentials.fromStream(credentials.getInputStream())
                        .createScoped(Collections.singleton(DriveScopes.DRIVE_FILE));

        return new Drive.Builder(httpTransport, jsonFactory, new HttpCredentialsAdapter(googleCredentials))
                .setApplicationName("Flipkart Uploader POC")
                .build();
    }
}
