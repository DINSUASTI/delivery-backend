package com.prueba.delivery.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class IdempotencyKeyGenerator {

    public static String generateKey(String eventType, String source, String fileName,
                                   String fileType, byte[] fileContent) {
        try {
            String data = eventType + "|" + source + "|" + fileName + "|" + fileType + "|" +
                         (fileContent != null ? Integer.toString(fileContent.length) : "0");

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));

            // Convertir a hexadecimal
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating idempotency key", e);
        }
    }
}