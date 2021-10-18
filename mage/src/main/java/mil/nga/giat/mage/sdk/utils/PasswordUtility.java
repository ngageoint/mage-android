package mil.nga.giat.mage.sdk.utils;

import android.util.Base64;

import java.security.SecureRandom;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 *
 * Used to hash passwords securely
 *
 * @author wiedemanns
 */
public class PasswordUtility {

    private static final int iterations = 1000;
    private static final int saltLen = 32;
    private static final int desiredKeyLen = 256;

    /**
     * Computes a salted PBKDF2 hash of given plaintext password suitable for storing in a database. Empty passwords are not supported.
     *
     * @param password
     * @return
     * @throws Exception
     */
    public static String getSaltedHash(String password) throws Exception {
        byte[] salt = SecureRandom.getInstance("SHA1PRNG").generateSeed(saltLen);
        // store the salt with the password
        return Base64.encodeToString(salt, Base64.NO_WRAP) + "$" + hash(password, salt);
    }

    public static boolean equal(String password, String hash) throws Exception {
        if(hash == null || password == null) {
            return false;
        }

        String[] saltAndPass = hash.split("\\$");
        if (saltAndPass.length != 2) {
            throw new IllegalStateException("The stored password have the form 'salt$hash'");
        }
        String hashOfInput = hash(password, Base64.decode(saltAndPass[0], Base64.NO_WRAP));
        return hashOfInput.equals(saltAndPass[1]);
    }

    // using PBKDF2 from Sun
    private static String hash(String password, byte[] salt) throws Exception {
        if (password == null || password.length() == 0) {
            throw new IllegalArgumentException("Empty passwords are not supported.");
        }
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        SecretKey key = f.generateSecret(new PBEKeySpec(password.toCharArray(), salt, iterations, desiredKeyLen)
        );
        return Base64.encodeToString(key.getEncoded(), Base64.NO_WRAP);
    }
}
