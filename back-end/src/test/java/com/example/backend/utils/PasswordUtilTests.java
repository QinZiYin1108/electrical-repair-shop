package com.example.backend.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.junit.jupiter.api.Test;

class PasswordUtilTests {

    @Test
    void createsAndVerifiesBcryptHashes() {
        String hash = PasswordUtil.hashPassword("correct-password", "legacy-salt");

        assertTrue(hash.startsWith("$2"));
        assertTrue(PasswordUtil.matches("correct-password", hash, "legacy-salt"));
        assertFalse(PasswordUtil.matches("wrong-password", hash, "legacy-salt"));
        assertFalse(PasswordUtil.needsRehash(hash));
    }

    @Test
    void verifiesLegacySha256ForGradualMigration() throws Exception {
        String salt = "legacy-salt";
        byte[] digest =
                MessageDigest.getInstance("SHA-256")
                        .digest(("correct-password@" + salt).getBytes(StandardCharsets.UTF_8));
        StringBuilder legacyHash = new StringBuilder();
        for (byte value : digest) {
            legacyHash.append(String.format("%02x", value));
        }

        assertTrue(PasswordUtil.matches("correct-password", legacyHash.toString(), salt));
        assertTrue(PasswordUtil.needsRehash(legacyHash.toString()));
    }
}
