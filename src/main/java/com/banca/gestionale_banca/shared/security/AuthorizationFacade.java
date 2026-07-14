package com.banca.gestionale_banca.shared.security;

import com.banca.gestionale_banca.shared.constants.Ruoli;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthorizationFacade {

    public boolean isEmployee(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + Ruoli.EMPLOYEE));
    }

    public void verificaProprietario(String ownerKeycloakId, String keycloakId, boolean isEmployee, String messageIfNotAuthorized) {
        if (isEmployee) {
            return;
        }
        if (!ownerKeycloakId.equals(keycloakId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, messageIfNotAuthorized);
        }
    }
}
