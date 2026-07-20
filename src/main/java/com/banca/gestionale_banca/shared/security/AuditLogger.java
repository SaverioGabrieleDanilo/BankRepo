package com.banca.gestionale_banca.shared.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Traccia le azioni privilegiate compiute da ADMIN/EMPLOYEE su risorse di altri utenti
 * (conti, transazioni, altri utenti). Logger dedicato "AUDIT" (invece del logger della
 * classe chiamante) così può essere instradato/filtrato separatamente in produzione senza
 * toccare il codice, es. verso un appender/file diverso in logback.
 */
@Component
public class AuditLogger {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    public void log(String actorKeycloakId, String actorUsername, String action, String resourceType, Object resourceId) {
        AUDIT.info("actor={} ({}) action={} resource={}:{}", actorUsername, actorKeycloakId, action, resourceType, resourceId);
    }
}
