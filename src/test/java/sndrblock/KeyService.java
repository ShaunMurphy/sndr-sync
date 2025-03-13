package sndrblock;

import java.nio.charset.Charset;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import ssdp.message.SsdpConstants;

import com.google.protobuf.ByteString;
import com.sndr.crypto.Crypto;
import com.sndr.proto.SndrBlockProto.Request;
import com.sndr.proto.SndrBlockProto.Response;
import com.sndr.sync.Sndr_Proto;

public final class KeyService {
    private final com.sndr.crypto.EllipticCurveCrypto crypto = CryptoTemp.INSTANCE.createNewInstance();
    private final KeyManager keyManager = KeyManager.getInstance();
    public KeyService() {
    }
    
    public Sndr_Proto.KeyResponse.Builder generateKeyResponse_OLD(String account, String passphrase, String deviceUuid, PublicKey otherDeviceKey) {
        SecretKeySpec aesKey = crypto.generateRandomAesKey();
        
        String protectedKey = crypto.protectAESKey(aesKey, otherDeviceKey);
        byte[] protectedAESKey = protectedKey.getBytes(Charset.forName("UTF-8"));
 
        //Encrypt the user's keys using the passphrase.
        byte[] encryptedUserKey1 = keyManager.getEncryptedUserKeyPair(account, passphrase);
        byte[] encryptedUserKey2 = crypto.encrypt(aesKey, encryptedUserKey1);
        
        //Split the IV from the key.
        byte[] iv = Arrays.copyOfRange(encryptedUserKey2, 0, 16);
        byte[] encryptedKey = Arrays.copyOfRange(encryptedUserKey2, 16, encryptedUserKey2.length);
        
        KeyPair deviceKeys = keyManager.getDeviceKeys();
        
        Sndr_Proto.KeyResponse.Builder builder = Sndr_Proto.KeyResponse.newBuilder();
        builder.setVersion(SsdpConstants.PRODUCT_VERSION);
        builder.setWrappedAesKey(ByteString.copyFrom(protectedAESKey));
        builder.setKeyType(Crypto.getKeyType());
        builder.setIv(ByteString.copyFrom(iv));
        builder.setEncryptedKey(ByteString.copyFrom(encryptedKey));
        builder.setDeviceUUID(ByteString.copyFrom(deviceUuid.getBytes()));
        
        //The signature must be the last item on this.
        //Sign the response.
        byte[] signThis = builder.buildPartial().toByteArray();
        byte[] signature = crypto.signStringBase64(deviceKeys.getPrivate(), signThis);
        builder.setSignature(ByteString.copyFrom(signature));
        
        
        return builder;
    }
    
    
    public Response.Keys.Builder generateKeyResponse(String account, String passphrase, String remoteDeviceUuid, PublicKey remoteDeviceKey) {
        SecretKeySpec aesKey = crypto.generateRandomAesKey();
        
        String protectedKey = crypto.protectAESKey(aesKey, remoteDeviceKey);
        byte[] protectedAESKey = protectedKey.getBytes(Charset.forName("UTF-8"));
 
        //Encrypt the user's keys using the passphrase.
        byte[] encryptedUserKey1 = keyManager.getEncryptedUserKeyPair(account, passphrase);
        byte[] encryptedUserKey2 = crypto.encrypt(aesKey, encryptedUserKey1);

        //Split the IV from the key.
        byte[] iv = Arrays.copyOfRange(encryptedUserKey2, 0, 16);
        byte[] encryptedKey = Arrays.copyOfRange(encryptedUserKey2, 16, encryptedUserKey2.length);
        
        KeyPair deviceKeys = keyManager.getDeviceKeys();
        
        Response.Keys.Builder builder = Response.Keys.newBuilder();
        builder.setVersion(SsdpConstants.PRODUCT_VERSION);
        builder.setProtectedAesKey(ByteString.copyFrom(protectedAESKey));
        builder.setKeyType(Crypto.getKeyType());
        builder.setIv(ByteString.copyFrom(iv));
        builder.setEncryptedKeys(ByteString.copyFrom(encryptedKey));
        builder.setDeviceUUID(ByteString.copyFromUtf8(UUID.randomUUID().toString()));//TODO This needs to be this device's UUID.

        //The signature must be the last item on this.
        //Sign the response.
        byte[] signThis = builder.buildPartial().toByteArray();
        byte[] signature = crypto.signStringBase64(deviceKeys.getPrivate(), signThis);
        //builder.setSignature(ByteString.copyFrom(signature));

        return builder;
    }

    public Response.Keys.Builder generateKeyResponse(com.sndr.proto.SndrBlockProto.Request.Keys request) {
        String account = request.getUsername();
        String passphrase = "qwerty";
        String deviceUuid = request.getDeviceUUID().toString();
        if(request.getDevicePublicKey() == null) {
            System.err.println("Missing remote device public key.");
        }
        PublicKey otherDeviceKey = crypto.importPublicKey(request.getDevicePublicKey().toStringUtf8());
        
        return generateKeyResponse(account, passphrase, deviceUuid, otherDeviceKey);
    }

    public Request.Keys.Builder generateKeyRequest(String account) {
        KeyPair deviceKeys = keyManager.getDeviceKeys();
        Request.Keys.Builder builder = Request.Keys.newBuilder();
        builder.setVersion(SsdpConstants.PRODUCT_VERSION);
        builder.setUsername(account);
        String devicePublicKey = crypto.exportPublicKey(deviceKeys.getPublic());
        builder.setDevicePublicKey(ByteString.copyFromUtf8(devicePublicKey));
        builder.setDeviceUUID(ByteString.copyFromUtf8(UUID.randomUUID().toString()));//TODO This needs to be this device's UUID.
        
        //The signature must be the last item on this.
        //Sign the response.
        byte[] signThis = builder.buildPartial().toByteArray();
        byte[] signature = crypto.signStringBase64(deviceKeys.getPrivate(), signThis);
        //builder.setSignature(ByteString.copyFrom(signature));
        
        return builder;
    }
}