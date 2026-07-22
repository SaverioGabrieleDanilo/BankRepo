package com.banca.gestionale_banca.utils;

import java.math.BigInteger;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

public class IbanGenerator {

    // Costruttore privato per impedire l'istanziamento della classe
    private IbanGenerator() {
        throw new IllegalStateException("Utility class");
    }

    public static String generateItalianIban() {
        String countryCode = "IT";
        Random random = new Random();

        // 1. Costruzione del BBAN Italiano (23 caratteri)
        // CIN: 1 lettera | ABI: 5 cifre | CAB: 5 cifre | Conto: 12 alfanumerici
        char cin = (char) ('A' + random.nextInt(26));
        String abi = String.format("%05d", random.nextInt(100000));
        String cab = String.format("%05d", random.nextInt(100000));
        String conto = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        String bban = cin + abi + cab + conto;

        // 2. Algoritmo Modulo 97 per trovare le 2 cifre di controllo
        String rearranged = bban + countryCode + "00";
        String numeric = rearranged.chars()
                .mapToObj(c -> Character.isDigit(c) ? String.valueOf((char) c) : String.valueOf(c - 'A' + 10))
                .collect(Collectors.joining());

        int checkDigits = 98 - new BigInteger(numeric).mod(BigInteger.valueOf(97)).intValue();

        // 3. Ritorna l'IBAN formattato (27 caratteri)
        return String.format("%s%02d%s", countryCode, checkDigits, bban);
    }
}
