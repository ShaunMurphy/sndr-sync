package sndrblock;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;

import com.sndr.api.UserApi;
import com.sndr.api.templates.ServerStatus;
import com.sndr.async.Call;
import com.sndr.crypto.EllipticCurveCrypto;
import com.sndr.gson.models.unversioned.IncomingUserDevice;
import com.sndr.gson.models.unversioned.IncomingUserLogin;
import com.sndr.httpclient.HttpRequest;
import com.sndr.httpclient.Response;

public enum Tester {
    INSTANCE;

    public static final String HOST = "https://a.sndr.com";
    //public static final String HOST = "https://dev.privategiant.com";
    public static final String keyPassword = "qwerty";
    public static final String user = "ReliableOpenEndedRam";
    /*
    public static final ServiceLink sndr = new ServiceLink(ServiceLink.ServiceType.sndr, user);
    public static final ServiceLink email = new ServiceLink(ServiceLink.ServiceType.mailto, "testaccount@privategiant.com");
    public static final ServiceLink sms = new ServiceLink(ServiceLink.ServiceType.sms, "5555555555");*/

    public static final String email = "testaccount@privategiant.com";

    public static final char[] loginPassword = "a".toCharArray();
    
    /** DO NOT MODIFY! **/
    private final String exportedUserKey = "N0EzM0UzMjlGREI2RkU1RDc4RDBENDRFNjcwNUM3OUM0OTY3MTEzRjY4NTE0RkFDNkM3"
            + "N0JCNjc4ODk0NzU1RCw4RDA5NkIxNzExREVERDUxNUI0RjY3QjUzQjgxRkNBMjQ2QkJGRDc4RUE2NzAxODNDNDBENzlGMkIyM"
            + "jQxMDg5LDFERDVCNDdEMjZBNTQ0NkU5RUZBODUxMUNGRTE5NDc1RDEwMkI4Q0M2MkY4RDc4NUExQkU5NTgxMzQ4RjA5OTQ=";
    /** DO NOT MODIFY! **/
    private final String exportedDeviceKey = "ODc5QkFGNjAzQTlDQUI0MjAwOUJBRDJGQjU3Rjg1OTNFOTEwRDAzMDk0NEE3N0FGND"
            + "hDNDc2RjYwNTkxNEMxQyw0NkU0ODZBRTgxMzI1OTUzNjlDMUE1MjkyQzI3RjQ4QjU2NkFDRjkxODVBNEVBRjRGN0M3MEJFNDU"
            + "3RkU4RDg3LDRDMTA5NUQyNjM5OTZERjVFNzU4N0FCRkI2QzkzMzMzRDU5OTI5RUE1OURGMThBOUI0NUM5QUE1ODgzNTE1OEE";      
    
    private final EllipticCurveCrypto crypto;
    private final KeyManager keyManager;
    private final KeyPair userKeys;
    private final KeyPair deviceKeys;
    
    private final String deviceId = "c00f4cc2-f68b-4ed8-8ab4-1a5ef4b3079b";
    private final File directory = new File("SNDR_SYNC_TEMP").getAbsoluteFile();
    private final String pushNotificationId = "1914428e3e23a4090b5efa18af853ce0dca0990202d90c024acce796c4d57d93a0b2e75d86dac07cb8ce05432dcacdeb";

    private Tester() {
        this.crypto = CryptoTemp.INSTANCE.createNewInstance();
        this.userKeys = this.crypto.importKeyPair(exportedUserKey);
        this.deviceKeys = this.crypto.importKeyPair(exportedDeviceKey);        
        if(!directory.exists()) {
            directory.mkdir();
        }
        this.keyManager = KeyManager.getInstance();
        this.keyManager.setDirectory(directory);
        
        //Add the user keys and device keys to the KeyManager.
        try {
            Field deviceKeyField = KeyManager.class.getDeclaredField("deviceKeys");
            deviceKeyField.setAccessible(true);
            deviceKeyField.set(this.keyManager, this.deviceKeys);
            
            Field userKeyPairMapField = KeyManager.class.getDeclaredField("userKeyPairMap");
            userKeyPairMapField.setAccessible(true);
            @SuppressWarnings("unchecked")
            HashMap<String, KeyPair> userKeyPairMap = ((HashMap<String, KeyPair>)userKeyPairMapField.get(keyManager));
            userKeyPairMap.put(Tester.user, this.userKeys);
            userKeyPairMap.put("mailto:testaccount@privategiant.com", this.userKeys);
            userKeyPairMap.put("sms:5555555555", this.userKeys);
        } catch (NoSuchFieldException | SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public final KeyManager getKeyManager() {
        return this.keyManager;
    }
    
    public final EllipticCurveCrypto getCrypto() {
        return this.crypto;
    }
    
    public final String getDeviceId() {
        return this.deviceId;
    }
    
    public final PublicKey getDevicePublicKey() {
        return this.deviceKeys.getPublic();
    }

    public final PrivateKey getDevicePrivateKey() {
        return this.deviceKeys.getPrivate();
    }

    public final File getDirectory() {
        return this.directory;
    }
    
    
    //Do not call this often. Its only needed when the account is logged out.
    public final void login() {
        IncomingUserLogin incomingUserLogin = new IncomingUserLogin();
        incomingUserLogin.uri = email;
        incomingUserLogin.service = "mailto:";
        incomingUserLogin.password = new String(loginPassword);
        incomingUserLogin.publicKey = crypto.exportPublicKey(this.userKeys.getPublic());

        IncomingUserDevice incomingUserDevice = new IncomingUserDevice();
        incomingUserDevice.deviceIdentifier = deviceId;
        incomingUserDevice.publicKey = crypto.exportPublicKey(deviceKeys.getPublic());
        incomingUserDevice.publicKeyType = "EC";
        incomingUserDevice.deviceType = "Desktop";
        incomingUserDevice.devicePlatform = "JavaFx";
        incomingUserDevice.displayName = "Sndr Desktop Client";
        incomingUserDevice.pushNotificationUniqueId = pushNotificationId;
        incomingUserDevice.isPushEnabled = true;
        incomingUserDevice.pushNotificationType = "SPNS_Rev_1";
        incomingUserLogin.device = incomingUserDevice;

        Call<HttpRequest<IncomingUserLogin>, Response<ServerStatus>> call = UserApi.loginUser(incomingUserLogin, null);
        try {
            Response<ServerStatus> response = call.execute();
            System.out.println(response.parsedBody);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}