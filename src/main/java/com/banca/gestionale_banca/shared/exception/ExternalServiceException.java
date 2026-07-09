package com.banca.gestionale_banca.shared.exception;

public class ExternalServiceException extends RuntimeException {

    private static final String DEFAULT_PUBLIC_MESSAGE = "Servizio esterno temporaneamente non disponibile";

    private final String publicMessage;

    public ExternalServiceException(String message) {
        super(message);
        this.publicMessage = DEFAULT_PUBLIC_MESSAGE;
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
        this.publicMessage = DEFAULT_PUBLIC_MESSAGE;
    }

    public String getPublicMessage() {
        return publicMessage;
    }
}