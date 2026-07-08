package com.banca.gestionale_banca.security;

import com.banca.gestionale_banca.constants.Ruoli;
import com.banca.gestionale_banca.model.BankAccount;
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

    public void verificaProprietario(BankAccount account, String keycloakId, boolean isEmployee, String messaggioSeNonAutorizzato) {
        if (isEmployee) {
            return;
        }
        if (!account.getUser().getKeycloakId().equals(keycloakId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, messaggioSeNonAutorizzato);
        }
    }
}
