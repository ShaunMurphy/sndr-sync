package application.interfaces;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public interface Crypto {    
    public SecretKeySpec generateRandomAesKey();
    public String protectAESKey(SecretKeySpec aesKey, PublicKey publicKey);
    public SecretKeySpec unprotectAESKey(String protectedKeyData, PrivateKey privateKey);
    public String exportPublicKey(PublicKey publicKey) throws IllegalArgumentException;
    public PublicKey importPublicKey(String base64PublicKey) throws IllegalArgumentException;
    
    public byte[] encrypt(final SecretKey aesKeySpec, final byte[] input);
    public byte[] decrypt(final SecretKey aesKeySpec, final byte[] input);
    public byte[] generateHMAC(final byte[] input, final SecretKeySpec aesKeySpec);
    public boolean verifyHMAC(final byte[] input, final byte[] hmac, final SecretKeySpec aesKeySpec);

    
    //These needs the device key.
    public byte[] signStringBase64(PrivateKey privateKey, byte[] data);
    
    public byte[] signStringBase64(byte[] data);
    //public boolean validateSignature(PublicKey publicKey, byte[] base64Signature, byte[] inputString);
    public boolean validateSignature(String deviceUuid, byte[] base64Signature, byte[] input);
    
    public String hashStringToSHABase64(String input);
    public String hashStringToMD5Base64(String input);

    public String getKeyType();
    public KeyPair getUserKeys(String account);
}
    
