package com.banca.gestionale_banca.config;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class KeycloakConfig {

    @Value("${keycloak.admin.username}")
    private String username;

    @Value("${keycloak.admin.password}")
    private String password;

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Bean
    public Keycloak keycloak() {
        log.info("Configurazione Keycloak admin client -> server: {}, realm login: master, clientId: admin-cli", serverUrl);

        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm("master")
                .clientId("admin-cli")
                .username(username)
                .password(password)
                .build();
    }
}