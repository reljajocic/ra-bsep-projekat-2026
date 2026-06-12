package com.example.bsep.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class InputValidator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Dozvoljava slova, brojeve, razmak, tacku, crticu, donju crtu i osnovne DN znakove
    private static final Pattern SAFE_TEXT_PATTERN =
            Pattern.compile("^[\\p{L}0-9 ._@*.,()-]{1,200}$");

    // Two-letter country code
    private static final Pattern COUNTRY_PATTERN = Pattern.compile("^[A-Za-z]{2}$");

    public void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email je obavezan");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Email nije u ispravnom formatu");
        }
    }

    public void validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " je obavezno polje");
        }
    }

    // Proverava da string ne sadrzi opasne znakove (XSS / injection osnovna zastita)
    public void validateSafeText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return; // opciona polja
        }
        if (!SAFE_TEXT_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    fieldName + " sadrzi nedozvoljene znakove");
        }
    }

    public void validateCountry(String country) {
        if (country == null || country.isBlank()) {
            return;
        }
        if (!COUNTRY_PATTERN.matcher(country).matches()) {
            throw new IllegalArgumentException("Country mora biti dvoslovni kod (npr. RS)");
        }
    }

    public void validateValidityDays(int days) {
        if (days < 1 || days > 7300) {
            throw new IllegalArgumentException("Trajanje mora biti izmedju 1 i 7300 dana");
        }
    }

    // Uklanja opasne HTML znakove iz stringa (sanitizacija)
    public String sanitize(String input) {
        if (input == null) return null;
        return input
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .trim();
    }
}