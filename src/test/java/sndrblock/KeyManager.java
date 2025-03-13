package sndrblock;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.util.HashMap;

import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Hex;

import com.sndr.crypto.Crypto;
import com.sndr.crypto.EllipticCurveCrypto;

/**
 * A manager to store the device keys and user keys.
 * 
 * @author shaun
 */
public final class KeyManager {
    private static KeyManager instance;
    private final EllipticCurveCrypto crypto;
    private File directory;
    private final HashMap<String, KeyPair> userKeyPairMap = new HashMap<String, KeyPair>();

    private KeyPair deviceKeys = null;

    private KeyManager() {
        this.crypto = CryptoTemp.INSTANCE.createNewInstance();
    }

    public final void setDirectory(File directory) {
        this.directory = directory;
    }

    public static KeyManager getInstance() {
        if (instance == null) {
            instance = new KeyManager();
        }
        return instance;
    }

    /**
     * Returns the device key pair. Loads the device key pair from disk if they
     * are not already in memory.
     * 
     * @return
     */
    public final KeyPair getDeviceKeys() {
        if (deviceKeys == null) {
            // Load the keys from disk.
            deviceKeys = crypto.loadKeyPairFromFile(getDeviceKeyFile());
            // System.out.println("--------- LOADED device keys ----------------");
        }
        // System.out.println("device private key:");
        // System.out.println(Base64.encodeToString(deviceKeys.getPrivate().getEncoded(),
        // Base64.NO_WRAP | Base64.URL_SAFE));
        return deviceKeys;
    }

    /**
     * Generates a device key pair and stores it on disk.
     * 
     * @return KeyPair The key pair that was generated.
     */
    private final KeyPair generateDeviceKeys() {
        // Create the device keys.
        // KeyPair deviceKeys = crypto.generateRSA_KeyPair();
        KeyPair deviceKeys = crypto.generateEC_KeyPair();
        // Store them.
        crypto.storeKeyPairToFile(getDeviceKeyFile(), deviceKeys);
        return deviceKeys;
    }

    /**
     * Returns the device keys. If they do not exist, this will create them.
     * 
     * @param overrideExisting
     *            - If true, this will override any existing keys in the
     *            provided directory.
     * @return
     */
    public final KeyPair getOrGenerateDeviceKeys(boolean overrideExisting) {
        // Return the device keys from disk if the device key file exists
        // and override existing is false.
        if (!overrideExisting && getDeviceKeyFile().exists()) {
            KeyPair deviceKeys = getDeviceKeys();
            if (deviceKeys != null) {
                return deviceKeys;
            }
        }
        // The keys do not exist, the file does not exist, or override existing
        // is true.
        this.deviceKeys = generateDeviceKeys();
        return this.deviceKeys;
    }

    /**
     * Returns the file used for the device keys.
     * 
     * @return
     */
    private final File getDeviceKeyFile() {
        String filename = crypto.hashStringToSHABase64("device_key_file");
        filename = filename.replaceAll("=", "");
        return new File(directory, filename);
    }

    private final File getUserKeyFile(final String user) {
        String filename = crypto.hashStringToSHABase64(user + "_keys");
        filename = filename.replaceAll("=", "");
        File file = new File(directory, filename);
        return file;
    }

    private SecretKeySpec generateKeyForFile(String passphrase) {
        byte[] salt = "Protect your information".getBytes();
        final int iterationCount = 777;
        SecretKeySpec aesKey = crypto.deriveAesKey(passphrase, salt, iterationCount,
                Crypto.AES_KEY_SIZE);
        return aesKey;
    }

    public final boolean storeUserKeyPair(final String user, final KeyPair keys, String passphrase) {
        // Store the keys to disk.
        SecretKeySpec aesKey = generateKeyForFile(passphrase);
        String exportKeyPair = crypto.exportKeyPair(keys);
        byte[] encryptedKey = crypto.encrypt(aesKey, exportKeyPair);
        try (FileOutputStream fos = new FileOutputStream(getUserKeyFile(user));
                DataOutputStream dos = new DataOutputStream(fos);) {
            dos.writeInt(encryptedKey.length);
            dos.write(encryptedKey);
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public final KeyPair loadUserKeyPair(final String user, String passphrase) {
        // Load the keys from disk.
        SecretKeySpec aesKey = generateKeyForFile(passphrase);
        byte[] exportedKeyPair = null;

        try (FileInputStream fis = new FileInputStream(getUserKeyFile(user));
                DataInputStream dis = new DataInputStream(fis);) {
            int keyPairLength = dis.readInt();
            byte[] encryptedKeyPairBytes = new byte[keyPairLength];
            dis.read(encryptedKeyPairBytes);
            // Decrypt the exported KeyPair.
            exportedKeyPair = crypto.decrypt(aesKey, encryptedKeyPairBytes);
        } catch (FileNotFoundException e) {
            System.err.println("User key file not found for user " + user + ".");
            System.err.println(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Convert the exportedKeyPair.
        KeyPair keys = null;

        if (exportedKeyPair != null) {
            keys = crypto.importKeyPair(new String(exportedKeyPair, Charset.forName("UTF-8")));
        }
        if (keys != null) {
            // System.out.println("Loaded keys for "+user+"\n"+
            // Base64.encodeToString(keys.getPrivate().getEncoded(),
            // Base64.NO_WRAP | Base64.URL_SAFE));
            userKeyPairMap.put(user, keys);
        }
        return keys;
    }

    public final KeyPair getUserKeyPair(final String user) {
        return userKeyPairMap.get(user);
    }

    public final byte[] getEncryptedUserKeyPair(final String user, final String passphrase) {
        KeyPair keys = getUserKeyPair(user);
        if(keys == null) {
            throw new IllegalStateException("The user "+user+" keys were not loaded.");
        }
        String exported = crypto.exportKeyPair(keys);
        SecretKeySpec aesKey = generateKeyForFile(passphrase);
        byte[] encryptedKeyPair = crypto.encrypt(aesKey, exported);
        return encryptedKeyPair;
    }

    public final KeyPair getDecryptedUserKeyPair(final byte[] encryptedKeyPair,
            final String passphrase) {
        SecretKeySpec aesKey = generateKeyForFile(passphrase);
        byte[] decrypted = crypto.decrypt(aesKey, encryptedKeyPair);
        String exported = new String(decrypted, Charset.forName("UTF-8"));
        KeyPair keys = crypto.importKeyPair(exported);
        return keys;
    }

    /**
     * Removes the user keys from the disk and memory.
     * 
     * @param user
     */
    public final void removeUserKeyPair(final String user) {
        // Remove the keys from the user key map.
        userKeyPairMap.remove(user);
        File keyFile = getUserKeyFile(user);
        safeDeleteFile(keyFile);
    }

    /**
     * Randomly writes bytes over a file's disk space.
     * 
     * @param file
     */
    private static final void safeDeleteFile(File file) {
        // Write random bytes to file before deletion
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "rws");
            long length = file.length();
            SecureRandom random = new SecureRandom();
            raf.seek(0);
            raf.getFilePointer();
            byte[] data = new byte[1024];
            int pos = 0;
            while (pos < length) {
                random.nextBytes(data);
                raf.write(data);
                pos += data.length;
            }
            raf.close();
        } catch (IOException e) {
            System.err.println("Couldn't write random bytes");
        }
        file.delete();
    }

    /**
     * Changes the passphrase on the user key file.
     * 
     * @param user
     * @param currentPassphrase
     * @param newPassphrase
     */
    public final void changePassphrase(String user, String currentPassphrase, String newPassphrase) {
        KeyPair keys = loadUserKeyPair(user, currentPassphrase);
        if (keys == null) {
            new IllegalArgumentException(
                    "The current passphrase was incorrect, nothing was changed.").printStackTrace();
            return;
        }
        storeUserKeyPair(user, keys, newPassphrase);
    }

    /**
     * Creates a new user KeyPair. This does not store them.
     * 
     * @return
     */
    public final KeyPair generateUserKeys() {
        KeyPair userKeyPair = crypto.generateEC_KeyPair();
        return userKeyPair;
    }

    public final String getKeyRecoveryFileName(final String user) {
        byte[] userHash = crypto.hashStringToSHA(user);
        String hex = new String(Hex.encode(userHash), StandardCharsets.UTF_8);
        String name = hex.substring(hex.length() - 8);
        return "DO_NOT_DELETE_SNDR_BACKUP_" + name + ".sndr";
    }

    // Wipe keys, write random bytes over them.
    // TODO Extend KeyPair, PrivateKey, and PublicKey to implement the destroy
    // interface to override the encoded bytes.
    // public void nukeKeys() {
    // //Arrays.fill(wrappedKey, (byte) 0);
    // deviceKeys = new KeyPair(null, null);
    // }

    // public final KeyStore loadKeyStore(final File keyStoreFile, final String
    // password) {
    // KeyStore keystore = null;
    // SecurityManager manager = new SecurityManager(keyStoreFile,
    // password.toCharArray());
    // keystore = manager.getKeyStore();
    // return keystore;
    // }
    //
    // public final void createKeyStore(final File keyStoreFile, final String
    // password, File certificateFile) {
    // SecurityManager manager = new SecurityManager(keyStoreFile,
    // password.toCharArray());
    // //KeyStore keyStore = manager.getKeyStore();
    // manager.loadCertificateToKeyStore(certificateFile);
    // manager.saveKeyStore(keyStoreFile, password.toCharArray());
    // }
}
