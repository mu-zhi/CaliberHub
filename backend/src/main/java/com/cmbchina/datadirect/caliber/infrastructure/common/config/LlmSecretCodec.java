package com.cmbchina.datadirect.caliber.infrastructure.common.config;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class LlmSecretCodec {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final LlmPreprocessProperties llmPreprocessProperties;
    private final SecureRandom secureRandom;

    public LlmSecretCodec(LlmPreprocessProperties llmPreprocessProperties) {
        this.llmPreprocessProperties = llmPreprocessProperties;
        this.secureRandom = new SecureRandom();
    }

    public String encrypt(String plainText) {
        String safe = plainText == null ? "" : plainText.trim();
        if (safe.isEmpty()) {
            return "";
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, buildKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(safe.getBytes(StandardCharsets.UTF_8));

            byte[] payload = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, payload, IV_LENGTH, encrypted.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to encrypt llm api key", ex);
        }
    }

    public String decrypt(String cipherText) {
        String safe = cipherText == null ? "" : cipherText.trim();
        if (safe.isEmpty()) {
            return "";
        }
        try {
            byte[] payload = Base64.getDecoder().decode(safe);
            if (payload.length <= IV_LENGTH) {
                return "";
            }

            byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(payload, IV_LENGTH, payload.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, buildKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] plain = cipher.doFinal(encrypted);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to decrypt llm api key", ex);
        }
    }

    private SecretKeySpec buildKey() {
        try {
            String secret = llmPreprocessProperties.getSecretKey();
            if (secret == null || secret.isBlank()) {
                throw new IllegalStateException("llm preprocess secret-key is not configured");
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(Arrays.copyOf(keyBytes, 16), "AES");
        } catch (Exception ex) {
            throw new IllegalStateException("failed to build secret key", ex);
        }
    }
}
