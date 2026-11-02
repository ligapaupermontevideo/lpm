package com.liga.calendar;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CedulaValidatorTest {

    @Test
    void acceptsFormattedValidCedula() {
        assertThat(CedulaValidator.isValid("1.234.567-2")).isTrue();
    }

    @Test
    void acceptsUnformattedValidCedula() {
        assertThat(CedulaValidator.isValid("41234563")).isTrue();
    }

    @Test
    void acceptsSevenDigitFormPaddedWithLeadingZero() {
        assertThat(CedulaValidator.isValid("1234561")).isTrue();
    }

    @Test
    void rejectsWrongCheckDigit() {
        assertThat(CedulaValidator.isValid("1.234.567-9")).isFalse();
    }

    @Test
    void rejectsTooShort() {
        assertThat(CedulaValidator.isValid("123456")).isFalse();
    }

    @Test
    void rejectsTooLong() {
        assertThat(CedulaValidator.isValid("123456789")).isFalse();
    }

    @Test
    void rejectsNonNumeric() {
        assertThat(CedulaValidator.isValid("abcdefgh")).isFalse();
    }

    @Test
    void rejectsNull() {
        assertThat(CedulaValidator.isValid(null)).isFalse();
    }

    @Test
    void normalizeStripsPunctuationAndPadsSevenDigitForm() {
        assertThat(CedulaValidator.normalize("1.234.567-2")).isEqualTo("12345672");
        assertThat(CedulaValidator.normalize("1234561")).isEqualTo("01234561");
    }
}
