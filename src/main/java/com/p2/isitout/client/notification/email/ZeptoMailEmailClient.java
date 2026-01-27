package com.p2.isitout.client.notification.email;

import com.p2.isitout.client.notification.email.dto.ZeptoMailRequest;
import com.p2.isitout.client.notification.email.dto.ZeptoMailResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ZeptoMailEmailClient {

    private final WebClient webClient;

    public ZeptoMailEmailClient(@Qualifier("zeptoMailClient") WebClient.Builder webClientBuilder, @Value("${zeptomail.api-key}") String apiKey) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.zeptomail.in/v1.1")
                .defaultHeader("Authorization", apiKey)
                .build();
    }

    public ZeptoMailResponse sendMail(ZeptoMailRequest request) {
        return webClient.post()
                .uri("/email")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ZeptoMailResponse.class)
                .block(); // OK inside scheduler/service
    }
}
