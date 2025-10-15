/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.util;

import com.zehro_mc.pokenotifier.PokeNotifier;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * A utility class for encrypting and decrypting player progress data using AES.
 * This prevents casual tampering with player progress files.
 */
public class DataSecurityUtil {

    // A hardcoded, non-obvious secret key. For this to be truly secure, it should not be a simple string.
    // This key will be hashed to ensure it fits the required key length for AES.
    private static final String SECRET_PHRASE = "p0k3-n0t1f13r_pr3st1g3-s3cur1ty-k3y_v1.0";
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static SecretKeySpec secretKey;
    private static IvParameterSpec ivParameterSpec;

    static {
        try {
            // Use a hashing algorithm to derive a key of a consistent length (e.g., 256-bit for AES)
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] key = sha.digest(SECRET_PHRASE.getBytes(StandardCharsets.UTF_8));
            secretKey = new SecretKeySpec(key, "AES");

            // For CBC mode, an Initialization Vector (IV) is needed. We can derive it from the key as well.
            // The IV should be 16 bytes for AES.
            byte[] iv = new byte[16];
            System.arraycopy(key, 0, iv, 0, iv.length);
            ivParameterSpec = new IvParameterSpec(iv);
        } catch (Exception e) {
            PokeNotifier.LOGGER.error("Failed to initialize cryptographic keys for data security!", e);
        }
    }

    /**
     * Encrypts a plain text string.
     * @param plainText The string to encrypt.
     * @return A Base64 encoded, encrypted string.
     */
    public static String encrypt(String plainText) {
        if (secretKey == null) return plainText; // Fail gracefully if crypto init failed
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(cipherText);
        } catch (Exception e) {
            PokeNotifier.LOGGER.error("Failed to encrypt data", e);
            return null; // Return null on failure
        }
    }

    /**
     * Decrypts an encrypted, Base64 encoded string.
     * @param cipherText The encrypted string.
     * @return The original plain text string, or null if decryption fails.
     */
    public static String decrypt(String cipherText) {
        if (secretKey == null) return cipherText; // Fail gracefully
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
            byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // This is expected if the file was tampered with or is from a different version.
            // We don't need to spam the log with errors in this case.
            return null;
        }
    }
}