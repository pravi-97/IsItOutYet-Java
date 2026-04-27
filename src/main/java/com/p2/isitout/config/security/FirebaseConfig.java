package com.p2.isitout.config.security;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.service-account}")
    private String firebaseServiceAccountJson;

    @PostConstruct
    public void init() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        InputStream is = new ByteArrayInputStream(
                firebaseServiceAccountJson.getBytes(StandardCharsets.UTF_8)
        );

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(is))
                .build();

        FirebaseApp.initializeApp(options);
    }
}
