package com.cmbchina.datadirect.caliber.infrastructure.common.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class LlmPromptFingerprint {

    private LlmPromptFingerprint() {
    }

    public static String of(String... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            if (parts != null) {
                for (String part : parts) {
                    digest.update(safe(part).getBytes(StandardCharsets.UTF_8));
                    digest.update((byte) 0x1F);
                }
            }
            String hex = HexFormat.of().formatHex(digest.digest());
            return hex.length() <= 16 ? hex : hex.substring(0, 16);
        } catch (NoSuchAlgorithmException ex) {
            return "unknown";
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

