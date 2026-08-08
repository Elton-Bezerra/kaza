package com.br.bz.kaza.kaza.service;

import org.springframework.stereotype.Component;

@Component
public class BrazilianTaxIdValidator {
    public boolean isValid(String taxId) {
        if (taxId == null || taxId.isBlank()) {
            return true;
        }
        return taxId.length() == 11 ? validCpf(taxId) : taxId.length() == 14 && validCnpj(taxId);
    }

    private boolean validCpf(String value) {
        if (!digitsOnly(value) || allSame(value)) {
            return false;
        }
        int first = cpfDigit(value, 9, 10);
        int second = cpfDigit(value, 10, 11);
        return first == value.charAt(9) - '0' && second == value.charAt(10) - '0';
    }

    private int cpfDigit(String value, int length, int weight) {
        int sum = 0;
        for (int index = 0; index < length; index++) {
            sum += (value.charAt(index) - '0') * (weight - index);
        }
        int remainder = 11 - (sum % 11);
        return remainder >= 10 ? 0 : remainder;
    }

    private boolean validCnpj(String value) {
        if (!digitsOnly(value) || allSame(value)) {
            return false;
        }
        int first = cnpjDigit(value, 12);
        int second = cnpjDigit(value, 13);
        return first == value.charAt(12) - '0' && second == value.charAt(13) - '0';
    }

    private int cnpjDigit(String value, int length) {
        int[] weights = length == 12
                ? new int[] {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}
                : new int[] {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int index = 0; index < length; index++) {
            sum += (value.charAt(index) - '0') * weights[index];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private boolean digitsOnly(String value) {
        return value.chars().allMatch(Character::isDigit);
    }

    private boolean allSame(String value) {
        return value.chars().allMatch(character -> character == value.charAt(0));
    }
}
