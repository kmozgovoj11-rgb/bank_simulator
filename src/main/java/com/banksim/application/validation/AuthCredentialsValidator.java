package com.banksim.application.validation;

import java.util.regex.Pattern;

/**
 * Правила для логина (телефон РФ) и пароля в сценариях {@link com.banksim.application.service.AuthService}.
 * <p>
 * <b>Логин.</b> Ввод трактуется как мобильный номер: допускаются привычные форматы ({@code +7…}, {@code 8…}, {@code 9…} и т.д.),
 * из строки извлекаются только цифры и приводятся к каноническому виду {@code +7} и ровно 10 следующих цифр
 * (итого 11 цифр национальной части, начинается с {@code 7}). Так один и тот же абонент не заведётся дважды из-за разного набора скобок и пробелов.
 * <p>
 * <b>Пароль.</b> Общие ограничения (длина сверху, пробелы, управляющие символы) одинаковы для регистрации и входа.
 * Минимальная длина проверяется только при регистрации — при входе короткий пароль не отсекается заранее,
 * чтобы не подсказывать атакующему «порог» длины до сравнения хеша.
 */
public final class AuthCredentialsValidator {

    private static final Pattern NORMALIZED_LOGIN = Pattern.compile("^\\+7\\d{10}$");

    private static final int PASSWORD_MIN = 6;
    private static final int PASSWORD_MAX = 128;

    private AuthCredentialsValidator() {}

    /**
     * Нормализует строку логина к виду {@code +7XXXXXXXXXX} или бросает {@link IllegalArgumentException}.
     * <ul>
     *   <li>10 цифр — считаем кодом без страны (например {@code 9001234567}) и подставляем ведущую {@code 7};</li>
     *   <li>11 цифр с {@code 8} — заменяем на {@code 7…} (формат {@code 8XXXXXXXXXX});</li>
     *   <li>11 цифр с {@code 7} — уже «полный» национальный номер;</li>
     *   <li>иначе — ошибка (не мобильный РФ в ожидаемом виде).</li>
     * </ul>
     * Нецифровые символы отбрасываются до разбора; пустая строка после этого — ошибка.
     *
     * @return канонический логин, например {@code +79001234567}
     */
    public static String normalizeAndValidateLogin(String rawLogin) {
        if (rawLogin == null) {
            throw new IllegalArgumentException("Login (phone) is required");
        }
        String trimmed = rawLogin.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Login (phone) cannot be empty");
        }

        String digitsOnly = trimmed.replaceAll("\\D", "");
        if (digitsOnly.isEmpty()) {
            throw new IllegalArgumentException("Login must contain a phone number (digits)");
        }

        String national;
        if (digitsOnly.length() == 10) {
            national = "7" + digitsOnly;
        } else if (digitsOnly.length() == 11 && digitsOnly.charAt(0) == '8') {
            national = "7" + digitsOnly.substring(1);
        } else if (digitsOnly.length() == 11 && digitsOnly.charAt(0) == '7') {
            national = digitsOnly;
        } else {
            throw new IllegalArgumentException(
                    "Phone must be 10 digits (e.g. 9001234567), or 11 digits starting with 7 or 8 (e.g. 79001234567)");
        }

        if (national.length() != 11 || national.charAt(0) != '7') {
            throw new IllegalArgumentException("Invalid Russian mobile phone number");
        }

        String canonical = "+" + national;
        if (!NORMALIZED_LOGIN.matcher(canonical).matches()) {
            throw new IllegalArgumentException("Login must be a valid phone in the form +7XXXXXXXXXX");
        }
        return canonical;
    }

    /**
     * Пароль при регистрации (и при смене пароля, если такой сценарий появится): общие правила плюс минимальная длина
     * (см. {@link #PASSWORD_MIN}, сейчас 6 символов).
     */
    public static void validatePassword(String plainPassword) {
        validatePasswordCommon(plainPassword);
        int len = plainPassword.length();
        if (len < PASSWORD_MIN) {
            throw new IllegalArgumentException("Password must be at least " + PASSWORD_MIN + " characters");
        }
    }

    /**
     * Пароль при попытке входа: только «гигиена» — null, только пробелы, слишком длинная строка, управляющие символы.
     * Минимальная длина <em>не</em> проверяется: заведомо неверный короткий пароль дойдёт до сравнения хеша и даст тот же
     * пустой результат, что и неверный длинный, без лишней подсказки проверяющему.
     */
    public static void validatePasswordForLoginAttempt(String plainPassword) {
        validatePasswordCommon(plainPassword);
    }

    /** Общая часть проверок пароля для регистрации и входа (верхняя граница длины, пробелы, control chars). */
    private static void validatePasswordCommon(String plainPassword) {
        if (plainPassword == null) {
            throw new IllegalArgumentException("Password is required");
        }
        if (plainPassword.length() > PASSWORD_MAX) {
            throw new IllegalArgumentException("Password must be at most " + PASSWORD_MAX + " characters");
        }
        if (plainPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be only spaces");
        }
        if (plainPassword.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Password cannot contain control characters");
        }
    }
}
