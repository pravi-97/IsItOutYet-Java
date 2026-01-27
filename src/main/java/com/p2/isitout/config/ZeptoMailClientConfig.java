package com.p2.isitout.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ZeptoMailClientConfig {

    @Bean
    @Qualifier("zeptoMailClient")
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
