package com.payroll.service;

import com.payroll.DAO.ITDAO;
import com.payroll.domain.IT;
import com.payroll.util.TwoFactorAuth;
import com.payroll.validation.OperationResult;
import com.payroll.validation.Password;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class AuthService {

    private static final Logger LOGGER = Logger.getLogger(AuthService.class.getName());

    static final int MAX_FAILED_ATTEMPTS = 5;
    static final Duration LOCKOUT = Duration.ofMinutes(5);

    private static final String DUMMY_HASH = Password.hashPassword("scootph-timing-equaliser-" + System.nanoTime());

    public enum Outcome { SUCCESS, TWO_FACTOR_REQUIRED, INVALID_CREDENTIALS, LOCKED }

    public record LoginResult(Outcome outcome, IT account, Duration retryAfter) {
    }

    public record TwoFactorEnrollment(String secret, String otpAuthUrl) {
    }

    private record Attempts(int failures, Instant lockedUntil, Instant lastFailure) {
    }

    private final ITDAO accounts;
    private final Clock clock;
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    public AuthService(ITDAO accounts) {
        this(accounts, Clock.systemDefaultZone());
    }

    public AuthService(ITDAO accounts, Clock clock) {
        this.accounts = accounts;
        this.clock = clock;
    }

    public LoginResult login(String username, char[] password) {
        String key = username == null ? "" : username.trim().toLowerCase();
        Duration locked = lockedFor(key);
        if (!locked.isZero()) {
            return new LoginResult(Outcome.LOCKED, null, locked);
        }
        if (key.isEmpty() || password == null || password.length == 0) {
            return new LoginResult(Outcome.INVALID_CREDENTIALS, null, Duration.ZERO);
        }

        String plain = new String(password);
        ITDAO.Credentials credentials = accounts.findCredentials(key);
        boolean valid;
        if (credentials == null) {
            Password.verifyPassword(plain, DUMMY_HASH);
            valid = false;
        } else {
            valid = verifyAndUpgrade(credentials, plain);
        }

        if (!valid) {
            return failed(key);
        }
        IT account = accounts.getAccountById(credentials.accountId());
        if (account == null) {
            return failed(key);
        }
        if (isTwoFactorEnabled(account)) {
            return new LoginResult(Outcome.TWO_FACTOR_REQUIRED, account, Duration.ZERO);
        }
        attempts.remove(key);
        return new LoginResult(Outcome.SUCCESS, account, Duration.ZERO);
    }

    public LoginResult verifyLoginCode(IT account, String code) {
        String key = account.getEmpUserName().trim().toLowerCase();
        Duration locked = lockedFor(key);
        if (!locked.isZero()) {
            return new LoginResult(Outcome.LOCKED, null, locked);
        }
        if (isCodeValid(account.getTfa(), code)) {
            attempts.remove(key);
            return new LoginResult(Outcome.SUCCESS, account, Duration.ZERO);
        }
        return failed(key);
    }

    private boolean verifyAndUpgrade(ITDAO.Credentials credentials, String plain) {
        String stored = credentials.passwordHash();
        if (stored == null || stored.isEmpty()) {
            return false;
        }
        if (isBcrypt(stored)) {
            return Password.verifyPassword(plain, stored);
        }
        boolean matches = MessageDigest.isEqual(stored.getBytes(StandardCharsets.UTF_8),
                plain.getBytes(StandardCharsets.UTF_8));
        if (matches) {
            accounts.storePasswordHash(credentials.accountId(), Password.hashPassword(plain));
            LOGGER.info("Upgraded a legacy plain-text password to BCrypt");
        }
        return matches;
    }

    static boolean isBcrypt(String value) {
        return value.matches("^\\$2[aby]?\\$\\d{2}\\$.{53}$");
    }

    private LoginResult failed(String key) {
        Instant now = clock.instant();
        if (attempts.size() > 1000) {
            attempts.values().removeIf(a -> a.lastFailure().plus(LOCKOUT).isBefore(now)
                    && (a.lockedUntil() == null || a.lockedUntil().isBefore(now)));
        }
        Attempts updated = attempts.merge(key, new Attempts(1, null, now), (old, one) -> {
            int failures = old.failures() + 1;
            return new Attempts(failures, failures >= MAX_FAILED_ATTEMPTS ? now.plus(LOCKOUT) : null, now);
        });
        if (updated.lockedUntil() != null) {
            return new LoginResult(Outcome.LOCKED, null, LOCKOUT);
        }
        return new LoginResult(Outcome.INVALID_CREDENTIALS, null, Duration.ZERO);
    }

    private Duration lockedFor(String key) {
        Attempts a = attempts.get(key);
        if (a == null || a.lockedUntil() == null) {
            return Duration.ZERO;
        }
        Duration remaining = Duration.between(clock.instant(), a.lockedUntil());
        if (remaining.isNegative() || remaining.isZero()) {
            attempts.remove(key);
            return Duration.ZERO;
        }
        return remaining;
    }

    public static boolean isTwoFactorEnabled(IT account) {
        return account != null && account.getTfa() != null && !account.getTfa().isBlank();
    }

    public TwoFactorEnrollment beginTwoFactorEnrollment(IT account) {
        String secret = TwoFactorAuth.generateSecretKey();
        return new TwoFactorEnrollment(secret, TwoFactorAuth.getQRBarcodeURL(account.getEmpUserName(), secret));
    }

    public void confirmTwoFactorEnrollment(IT account, TwoFactorEnrollment enrollment, String code) {
        if (!isCodeValid(enrollment.secret(), code)) {
            throw new ServiceException("That code doesn't match. Enter the current 6-digit code from your authenticator app.",
                    Map.of("code", "Invalid code"));
        }
        if (!accounts.updateTfaSecret(account.getAccountID(), enrollment.secret())) {
            throw new ServiceException("Could not save two-factor settings. Please try again.");
        }
        account.setTfa(enrollment.secret());
    }

    static boolean isCodeValid(String secret, String code) {
        if (secret == null || secret.isBlank() || code == null) {
            return false;
        }
        String digits = code.replaceAll("\\s", "");
        if (!digits.matches("\\d{6}")) {
            return false;
        }
        return TwoFactorAuth.isCodeValid(secret, Integer.parseInt(digits));
    }

    public IT verifyIdentity(String username, LocalDate birthday, String phone, String tin) {
        if (isBlank(username) || birthday == null || isBlank(phone) || isBlank(tin)) {
            throw new ServiceException("Please fill in all fields.");
        }
        String key = "reset:" + username.trim().toLowerCase();
        Duration locked = lockedFor(key);
        if (!locked.isZero()) {
            throw new ServiceException("Too many attempts. Try again in " + Math.max(1, locked.toMinutes() + 1) + " minutes.");
        }
        int accountId = accounts.verifyIdentity(username.trim(), birthday.toString(), phone.trim(), tin.trim());
        IT account = accountId > 0 ? accounts.getAccountById(accountId) : null;
        if (account == null) {
            if (failed(key).outcome() == Outcome.LOCKED) {
                throw new ServiceException("Too many attempts. Try again in " + LOCKOUT.toMinutes() + " minutes.");
            }
            throw new ServiceException("We couldn't verify your identity with those details.");
        }
        attempts.remove(key);
        return account;
    }

    public boolean verifyResetCode(IT account, String code) {
        return isCodeValid(account.getTfa(), code);
    }

    public void resetPassword(IT account, char[] newPassword, char[] confirmation) {
        String password = new String(newPassword);
        validateNewPassword(password, new String(confirmation));
        if (!accounts.updatePassword(account.getAccountID(), password)) {
            throw new ServiceException("Could not update the password. Please try again.");
        }
        attempts.remove(account.getEmpUserName().trim().toLowerCase());
    }

    public void changePassword(IT account, char[] current, char[] newPassword, char[] confirmation) {
        OperationResult result = new AccountService(accounts).changePassword(account,
                new String(current), new String(newPassword), new String(confirmation));
        if (!result.isSuccess()) {
            String field = result.getMessage().toLowerCase().contains("current") ? "current"
                    : result.getMessage().toLowerCase().contains("match") ? "confirm" : "password";
            throw new ServiceException(result.getFormattedErrors(), Map.of(field, result.getErrors().isEmpty()
                    ? result.getMessage() : String.join(" ", result.getErrors())));
        }
    }

    static void validateNewPassword(String password, String confirmation) {
        var weaknesses = Password.strengthErrors(password);
        if (!weaknesses.isEmpty()) {
            throw new ServiceException("Password requirements: " + String.join(" ", weaknesses),
                    Map.of("password", String.join(" ", weaknesses)));
        }
        if (!password.equals(confirmation)) {
            throw new ServiceException("Passwords do not match.", Map.of("confirm", "Passwords do not match"));
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
