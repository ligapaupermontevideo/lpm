package com.liga.calendar;

/**
 * Validates Uruguayan Cedula de Identidad numbers using the official check
 * digit algorithm (not just a format/length check), so a reservation can't
 * be made with a cedula number that doesn't actually exist.
 */
public final class CedulaValidator {

    // Weight applied to each of the 7 base digits, in order.
    private static final int[] WEIGHTS = {2, 9, 8, 7, 6, 3, 4};

    private CedulaValidator() {
    }

    /**
     * Strips everything but digits and pads to the standard 8-digit form
     * (7 base digits + 1 check digit), e.g. "1.234.567-8" and "234.567-8"
     * both normalize to "01234567" / "00234567" so the same person's cedula
     * always maps to the same lookup key regardless of how they typed it.
     * Returns the digits as-is (possibly not 8 long) if they don't fit that
     * shape — callers should check {@link #isValid} before trusting this.
     */
    public static String normalize(String rawCedula) {
        if (rawCedula == null) {
            return "";
        }
        String digits = rawCedula.replaceAll("\\D", "");
        if (digits.length() == 7) {
            digits = "0" + digits;
        }
        return digits;
    }

    public static boolean isValid(String rawCedula) {
        String digits = normalize(rawCedula);
        if (digits.length() != 8) {
            return false;
        }

        int sum = 0;
        for (int i = 0; i < 7; i++) {
            sum += (digits.charAt(i) - '0') * WEIGHTS[i];
        }
        int expectedCheckDigit = (10 - (sum % 10)) % 10;
        int actualCheckDigit = digits.charAt(7) - '0';

        return expectedCheckDigit == actualCheckDigit;
    }
}
