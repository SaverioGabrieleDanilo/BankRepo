package com.banca.gestionale_banca.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class KeycloakConfig {

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.service-client-id}")
    private String serviceClientId;

    @Value("${keycloak.service-client-secret}")
    private String serviceClientSecret;

    @Bean
    public Keycloak keycloak() {
        log.info("Configurazione Keycloak service client -> server: {}, realm: {}, clientId: {}", serverUrl, realm, serviceClientId);

        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(serviceClientId)
                .clientSecret(serviceClientSecret)
                .build();
    }
}
