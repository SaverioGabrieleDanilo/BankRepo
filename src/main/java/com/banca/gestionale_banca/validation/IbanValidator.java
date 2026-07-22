package com.banca.gestionale_banca.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigInteger;

public class IbanValidator implements ConstraintValidator<Iban, String> {

    // Regex rigorosa: IT + 2 cifre + 1 lettera + 10 cifre + 12 alfanumerici
    private static final String IT_IBAN_REGEX = "^IT[0-9]{2}[A-Z]{1}[0-9]{10}[A-Z0-9]{12}$";

    @Override
    public boolean isValid(String iban, ConstraintValidatorContext context) {
        // Deleghiamo a @NotNull il controllo sui campi nulli
        if (iban == null || iban.trim().isEmpty()) {
            return true; 
        }

        // Rimuoviamo eventuali spazi e uniformiamo a maiuscolo
        String cleanIban = iban.replaceAll("\\s+", "").toUpperCase();

        // 1. Controllo Sintattico (Lunghezza e Struttura)
        if (!cleanIban.matches(IT_IBAN_REGEX)) {
            return false;
        }

        // 2. Controllo Matematico (Modulo 97)
        // Spostiamo i primi 4 caratteri alla fine
        String rearranged = cleanIban.substring(4) + cleanIban.substring(0, 4);
        
        // Convertiamo le lettere in numeri (A=10, B=11...)
        StringBuilder numeric = new StringBuilder();
        for (char c : rearranged.toCharArray()) {
            if (Character.isDigit(c)) {
                numeric.append(c);
            } else {
                numeric.append(c - 'A' + 10);
            }
        }

        // Se il resto della divisione per 97 è 1, l'IBAN è valido
        BigInteger ibanNumber = new BigInteger(numeric.toString());
        return ibanNumber.mod(BigInteger.valueOf(97)).intValue() == 1;
    }
}