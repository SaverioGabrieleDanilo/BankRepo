package com.banca.gestionale_banca.shared.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException e) {
        return body(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException e) {
        return body(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("Violazione vincolo di integrità: {}", e.getMessage());
        return body(HttpStatus.CONFLICT, "Dato già esistente o vincolo violato");
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<Map<String, Object>> handleExternalService(ExternalServiceException e) {
        log.error("Errore servizio esterno: {}", e.getMessage(), e.getCause());
        return body(HttpStatus.BAD_GATEWAY, e.getPublicMessage());
    }

    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLock(Exception e) {
        return body(HttpStatus.CONFLICT, "Operazione concorrente rilevata sul conto, riprova");
    }

    @ExceptionHandler(org.springframework.dao.PessimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handlePessimisticLock(Exception e) {
        log.warn("Lock pessimistico non acquisito sul conto: {}", e.getMessage());
        return body(HttpStatus.CONFLICT, "Conto momentaneamente occupato da un'altra operazione, riprova");
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException e) {
        String expectedType = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "valido";
        return body(HttpStatus.BAD_REQUEST, "Parametro '" + e.getName() + "' non valido: atteso un valore di tipo " + expectedType);
    }

    // Copre sia i fallimenti di @PreAuthorize (AuthorizationDeniedException, sottoclasse
    // di AccessDeniedException dalla 6.3) sia eventuali AccessDeniedException "classiche".
    // Senza questo handler finiscono nel catch-all generico e tornano 500 invece di 403.
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(org.springframework.security.access.AccessDeniedException e) {
        return body(HttpStatus.FORBIDDEN, "Accesso negato: permessi insufficienti");
    }

    // I controlli manuali di proprietà (AuthorizationFacade, UserController) lanciano
    // ResponseStatusException con lo status già corretto: va solo rispettato, non
    // lasciato ricadere nel catch-all che lo trasformerebbe in 500.
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(org.springframework.web.server.ResponseStatusException e) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        String message = e.getReason() != null ? e.getReason() : e.getMessage();
        return body(status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    // @Valid fallito su un @RequestBody: senza questo handler, il catch-all generico
    // sotto la trasforma in 500 invece di 400 con il dettaglio dei campi non validi.
    // È il caso d'errore più frequente in un'API REST (input client sbagliato).
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(org.springframework.web.bind.MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("message", "Validazione fallita");
        body.put("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // Body JSON assente, malformato o con un tipo non convertibile (es. stringa dove
    // serve un numero). Stessa ragione degli altri: evitare che diventi un 500.
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedBody(org.springframework.http.converter.HttpMessageNotReadableException e) {
        return body(HttpStatus.BAD_REQUEST, "Corpo della richiesta assente o malformato");
    }

    // Verbo HTTP sbagliato su un path che esiste (es. GET su un endpoint solo POST).
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(org.springframework.web.HttpRequestMethodNotSupportedException e) {
        return body(HttpStatus.METHOD_NOT_ALLOWED, "Metodo '" + e.getMethod() + "' non supportato su questo endpoint");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception e) {
        log.error("Errore non gestito", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Errore interno del server");
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "message", message
        ));
    }
}
