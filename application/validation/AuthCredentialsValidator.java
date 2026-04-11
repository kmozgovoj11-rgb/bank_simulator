package application.validation;

import java.util.regex.Pattern;


public final class AuthCredentialsValidator {

    private static final Pattern NORMALIZED_LOGIN = Pattern.compile("^\\+7\\d{10}$");

    private static final int PASSWORD_MIN = 6;
    private static final int PASSWORD_MAX = 128;

    private AuthCredentialsValidator() {}

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

    public static void validatePassword(String plainPassword) {
        validatePasswordCommon(plainPassword);
        int len = plainPassword.length();
        if (len < PASSWORD_MIN) {
            throw new IllegalArgumentException("Password must be at least " + PASSWORD_MIN + " characters");
        }
    }


    public static void validatePasswordForLoginAttempt(String plainPassword) {
        validatePasswordCommon(plainPassword);
    }

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

