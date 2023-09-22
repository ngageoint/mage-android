package mil.nga.giat.mage.sdk.utils;

import android.util.Base64;

import java.security.SecureRandom;

import javax.annotation.Nullable;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordUtility {

    private static final int iterations = 1000;
    private static final int saltLen = 32;
    private static final int desiredKeyLen = 256;

    public static @Nullable String getSaltedHash(String password) {
        try {
            byte[] salt = SecureRandom.getInstance("SHA1PRNG").generateSeed(saltLen);
            // store the salt with the password
            return Base64.encodeToString(salt, Base64.NO_WRAP) + "$" + hash(password, salt);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean equal(String password, String hash) {
        if (hash == null || password == null) {
            return false;
        }

        try {
            String[] saltAndPass = hash.split("\\$");
            if (saltAndPass.length != 2) return false;
            String hashOfInput = hash(password, Base64.decode(saltAndPass[0], Base64.NO_WRAP));
            return hashOfInput.equals(saltAndPass[1]);
        } catch (Exception e) {
            return false;
        }
    }

    private static @Nullable String hash(String password, byte[] salt) {
        if (password == null || password.length() == 0) {
            return null;
        }

        try {
            SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            SecretKey key = f.generateSecret(new PBEKeySpec(password.toCharArray(), salt, iterations, desiredKeyLen));
            return Base64.encodeToString(key.getEncoded(), Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }
}
