package com.payroll.validation;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

public class SanitizationService {

    private static final PolicyFactory STRIP_ALL_HTML = new HtmlPolicyBuilder()
            .toFactory();

    private static final PolicyFactory BASIC_HTML = new HtmlPolicyBuilder()
            .allowElements("b", "i", "u", "em", "strong", "br", "p")
            .toFactory();

    public static String sanitizePlainText(String input) {
        if (input == null) {
            return null;
        }
        String sanitized = STRIP_ALL_HTML.sanitize(input);
        sanitized = unescapeHtml(sanitized);
        sanitized = sanitized.replaceAll("(?i)<script[^>]*>.*?</script>", "")
                             .replaceAll("(?i)javascript:", "")
                             .replaceAll("(?i)on\\w+\\s*=", "");
        return sanitized.trim();
    }

    @SuppressWarnings("deprecation")
    private static String unescapeHtml(String text) {
        return org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4(text);
    }

    public static String sanitizeBasicHtml(String input) {
        if (input == null) {
            return null;
        }

        return BASIC_HTML.sanitize(input).trim();
    }

    public static boolean isSafeSqlInput(String input) {
        if (input == null) {
            return true;
        }

        String lowercase = input.toLowerCase();

        String[] dangerousPatterns = {
            "union", "select", "insert", "update", "delete", "drop",
            "exec", "execute", "script", "--", "/*", "*/", "xp_", "sp_"
        };

        for (String pattern : dangerousPatterns) {
            if (lowercase.contains(pattern)) {
                return false;
            }
        }

        return true;
    }
}
