package com.payroll.util;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

public class TwoFactorAuth {
    private static final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    public static String generateSecretKey() {
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();
    }

    public static String getQRBarcodeURL(String username, String secret) {
        String issuer = "ScootPH";
        String label = java.net.URLEncoder.encode(issuer + ":" + username, java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");
        return "otpauth://totp/" + label + "?secret=" + secret + "&issuer=" + issuer;
    }

    public static boolean isCodeValid(String secret, int code) {
        return gAuth.authorize(secret, code);
    }
}
