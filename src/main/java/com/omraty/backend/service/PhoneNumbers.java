package com.omraty.backend.service;

/** Numéros de téléphone saisis par un admin, ramenés au format enregistré (+222XXXXXXXX). */
final class PhoneNumbers {

    private static final String MAURITANIA_PREFIX = "+222";
    private static final int LOCAL_NUMBER_LENGTH = 8;

    private PhoneNumbers() {}

    /**
     * Accepte « 36000000 », « 36 00 00 00 », « 22236000000 » ou « +222 36000000 » : seuls les
     * chiffres comptent ; 8 chiffres = numéro mauritanien sans indicatif.
     */
    static String normalize(String phone) {
        String digits = phone == null ? "" : phone.replaceAll("\\D", "");
        return digits.length() == LOCAL_NUMBER_LENGTH ? MAURITANIA_PREFIX + digits : "+" + digits;
    }
}
