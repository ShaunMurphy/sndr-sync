package sndrblock;

import icelink.model.SDP;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import application.interfaces.ApiHelper;
import application.interfaces.Crypto;
import application.interfaces.SyncMediator;

import com.sndr.api.DeviceApi;
import com.sndr.api.models.Connection;
import com.sndr.api.models.Connection.ConnectionDeviceCommand;
import com.sndr.api.templates.ConnectionTemplate;
import com.sndr.api.util.NetworkUtil;
import com.sndr.async.Call;
import com.sndr.gson.models.unversioned.connection.IncomingConnectionUpdate;
import com.sndr.gson.models.unversioned.connection.IncomingDeviceConnection;
import com.sndr.httpclient.HttpRequest;
import com.sndr.httpclient.util.Base64;
import com.sndr.proto.SndrBlockProto.Request;
import com.sndr.proto.SndrBlockProto.Response;

/**
 * DO NOT USE THIS CLASS IN PRODUCTION!
 */
public class Everything implements SyncMediator, Crypto, ApiHelper {
    
    private final com.sndr.crypto.EllipticCurveCrypto crypto = CryptoTemp.INSTANCE.createNewInstance();
    private final PrivateKey devicePrivateKey;
    private final PublicKey devicePublicKey;
    private final KeyManager manager;
    private final KeyService keyService = new KeyService();
    public final String user;
    Everything(String user, KeyManager manager) {
        this.user = user;
        this.manager = manager;
        devicePrivateKey = manager.getDeviceKeys().getPrivate();
        devicePublicKey = manager.getDeviceKeys().getPublic();
    }

    //SyncMediator
    @Override
    public Map<String, PublicKey> getUserDevicesKeys(String user) {
        return null;
    }
    
    @Override
    public byte[] getEncryptedUserKeyPair(String user) {
        return null;
    }
    
    @Override
    public KeyPair getDeviceKeys() {
        return null;
    }
    
    //Crypto
    @Override
    public byte[] signStringBase64(byte[] data) {
        return crypto.signStringBase64(devicePrivateKey, data);
    }

    @Override
    public byte[] signStringBase64(PrivateKey privateKey, byte[] data) {
        return crypto.signStringBase64(privateKey, data);
    }

    @Override
    public boolean validateSignature(String deviceUuid, byte[] base64Signature, byte[] input) {
        //The deivceUuid will be used to look up the correct public key.
        return crypto.validateSignature(devicePublicKey, base64Signature, input);
    }

    @Override
    public SecretKeySpec generateRandomAesKey() {
        return crypto.generateRandomAesKey();
    }

    @Override
    public String protectAESKey(SecretKeySpec aesKey, PublicKey publicKey) {
        return crypto.protectAESKey(aesKey, publicKey);
    }

    @Override
    public SecretKeySpec unprotectAESKey(String protectedKeyData, PrivateKey privateKey) {
        return crypto.unprotectAESKey(protectedKeyData, privateKey);
    }

    @Override
    public String exportPublicKey(PublicKey publicKey) throws IllegalArgumentException {
        return crypto.exportPublicKey(publicKey);
    }

    @Override
    public PublicKey importPublicKey(String base64PublicKey)
            throws IllegalArgumentException {
        return crypto.importPublicKey(base64PublicKey);
    }

    @Override
    public byte[] encrypt(SecretKey aesKeySpec, byte[] input) {
        return crypto.encrypt(aesKeySpec, input);
    }

    @Override
    public byte[] decrypt(SecretKey aesKeySpec, byte[] input) {
        return crypto.decrypt(aesKeySpec, input);
    }

    @Override
    public byte[] generateHMAC(byte[] input, SecretKeySpec aesKeySpec) {
        return crypto.generateHMAC(input, aesKeySpec);
    }

    @Override
    public boolean verifyHMAC(byte[] input, byte[] hmac, SecretKeySpec aesKeySpec) {
        return crypto.verifyHMAC(input, hmac, aesKeySpec);
    }

    @Override
    public String hashStringToSHABase64(String input) {
        return crypto.hashStringToSHABase64(input);
    }

    @Override
    public String hashStringToMD5Base64(String input) {
        return crypto.hashStringToMD5Base64(input);
    }
    
    @Override
    public String getKeyType() {
        return "ECC-AES256";
    }

    @Override
    public KeyPair getUserKeys(String account) {
        return manager.getUserKeyPair(this.user);
    }

    //ApiHelper
    @Override
    public Response.SendMessage sendMessage(com.sndr.proto.SndrBlockProto.Request.SendMessage message) {
        return null;
    }

    @Override
    public Response.StashNote stashNote(com.sndr.proto.SndrBlockProto.Request.StashNote note) {
        return null;
    }

    @Override
    public Response.StashFile stashFile(com.sndr.proto.SndrBlockProto.Request.StashFile file) {
        return null;
    }

    @Override
    public boolean isDeviceUuidValid(String deviceUuid) {
        return false;
    }

    @Override
    public List<File> generateThumbnails(List<File> file) {
        return null;
    }

    @Override
    public Response.Keys.Builder generateKeyResponse(com.sndr.proto.SndrBlockProto.Request.Keys request) {
        return keyService.generateKeyResponse(request);
    }

    @Override
    public Request.Keys.Builder generateKeyRequest(String account) {
        return keyService.generateKeyRequest(account);
    }

    @Override
    public final void getOffer(final SDP sdp) {
        if(sdp.connectionUuid == null || sdp.connectionUuid.isEmpty()) {
            throw new IllegalArgumentException("SDP connectionUuid is null or empty.");
        }

        com.sndr.async.Call<HttpRequest<com.sndr.httpclient.Response<Void>>, com.sndr.httpclient.Response<ConnectionTemplate>> call = 
                DeviceApi.getConnection(user, sdp.connectionUuid);
        try {
            com.sndr.httpclient.Response<ConnectionTemplate> response = call.execute();
            SDP.Offer offer = sdp.offer();
            ConnectionTemplate model = response.parsedBody;
            offer.encryptedJson = Base64.decode(model.encryptedOffer, Base64.NO_WRAP);
            offer.encryptedKey = model.encryptedOfferKey;
            offer.hmac = Base64.decode(model.offerHmac, Base64.NO_WRAP | Base64.URL_SAFE);
            offer.signature = Base64.decode(model.offerSignature, Base64.NO_WRAP | Base64.URL_SAFE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void putOffer(final SDP sdp) {
        Connection connection = new Connection();
        connection.command = ConnectionDeviceCommand.ALL;
        connection.type = com.sndr.api.models.Connection.ConnectionType.DEVICE;
        Calendar now = Calendar.getInstance();
        now.add(Calendar.MINUTE, 5);
        connection.timeout = now.getTime();

        SDP.Offer offer = sdp.offer();
        connection.encryptedOfferKey = offer.encryptedKey;
        connection.encryptedOffer = offer.encryptedJson;
        connection.offerHmac = offer.hmac;
        connection.offerSignature = offer.signature;
        connection.deviceUUID = sdp.remoteDeviceUuid;

        IncomingDeviceConnection incomingConnection = new IncomingDeviceConnection();
        incomingConnection.blockDeviceUUID = connection.deviceUUID;
        incomingConnection.commandType = connection.command.name();
        incomingConnection.encryptedOffer = Base64.encodeToString(connection.encryptedOffer, Base64.NO_WRAP);
        incomingConnection.encryptedOfferKey = connection.encryptedOfferKey;
        incomingConnection.signature = Base64.encodeToString(connection.offerSignature, Base64.NO_WRAP | Base64.URL_SAFE);
        incomingConnection.hmac = Base64.encodeToString(connection.offerHmac, Base64.NO_WRAP | Base64.URL_SAFE);
        incomingConnection.timeout = NetworkUtil.SERVER_DATE_FORMAT.format(connection.timeout);

        System.out.println("Sending offer to server for "+connection.deviceUUID);
        
        com.sndr.async.Call<HttpRequest<IncomingDeviceConnection>, com.sndr.httpclient.Response<ConnectionTemplate.ConnectionUUIDTemplate>> call = 
                DeviceApi.createDeviceConnection(user, incomingConnection);
        try {
            com.sndr.httpclient.Response<ConnectionTemplate.ConnectionUUIDTemplate> response = call.execute();
            if(response.parsedBody != null) {
                sdp.connectionUuid = response.parsedBody.connectionUUID;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

    @Override
    public void putAnswer(final SDP sdp) {
        Connection connection = new Connection();
        connection.command = ConnectionDeviceCommand.ALL;
        connection.type = com.sndr.api.models.Connection.ConnectionType.DEVICE;
        Calendar now = Calendar.getInstance();
        now.add(Calendar.MINUTE, 5);
        connection.timeout = now.getTime();
        connection.connectionUUID = sdp.connectionUuid;

        SDP.Answer answer = sdp.answer();
        connection.encryptedAnswerKey = answer.encryptedKey;
        connection.encryptedAnswer = answer.encryptedJson;
        connection.answerHmac = answer.hmac;
        connection.answerSignature = answer.signature;
        connection.deviceUUID = sdp.remoteDeviceUuid;

        IncomingConnectionUpdate incomingConnection = new IncomingConnectionUpdate();        
        incomingConnection.connectionUUID = connection.connectionUUID;
        incomingConnection.answerState = "???";
        incomingConnection.encryptedAnswer = Base64.encodeToString(connection.encryptedAnswer, Base64.NO_WRAP);
        incomingConnection.encryptedAnswerKey = connection.encryptedAnswerKey;
        incomingConnection.signature = Base64.encodeToString(connection.answerSignature, Base64.NO_WRAP | Base64.URL_SAFE);
        incomingConnection.hmac = Base64.encodeToString(connection.answerHmac, Base64.NO_WRAP | Base64.URL_SAFE);

        com.sndr.async.Call<HttpRequest<IncomingConnectionUpdate>, com.sndr.httpclient.Response<ConnectionTemplate>> call = 
                DeviceApi.updateConnection(user, incomingConnection);
        try {
            com.sndr.httpclient.Response<ConnectionTemplate> response = call.execute();
            //ConnectionTemplate connection = response.parsedBody;
            //answer.connectionUuid = connection.connectionUUID;        
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void getAnswer(final SDP sdp) {
        if(sdp.connectionUuid == null) {
            System.out.println("The connectionUuid is missing from the SDP parameter.");
            return;
        }

        Call<HttpRequest<com.sndr.httpclient.Response<Void>>, com.sndr.httpclient.Response<ConnectionTemplate>> call = 
                DeviceApi.getConnection(user, sdp.connectionUuid);

        try {
            com.sndr.httpclient.Response<ConnectionTemplate> response = call.execute();
            ConnectionTemplate model = response.parsedBody;
            SDP.Answer answer = sdp.answer();
            answer.encryptedJson = Base64.decode(model.encryptedAnswer, Base64.NO_WRAP);
            answer.encryptedKey = model.encryptedAnswerKey;
            answer.hmac = Base64.decode(model.answerHmac, Base64.NO_WRAP | Base64.URL_SAFE);
            answer.signature = Base64.decode(model.answerSignature, Base64.NO_WRAP | Base64.URL_SAFE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String toStringConnectionTemplate(ConnectionTemplate c) {
        return "ConnectionTemplate [connectionUUID=" + c.connectionUUID + ", resourceUUID="
                + c.resourceUUID + ", resourceType=" + c.resourceType + ", resourceSubType="
                + c.resourceSubType + ", deviceUUID=" + c.deviceUUID + ", encryptedOffer="
                + c.encryptedOffer + ", encryptedOfferKey=" + c.encryptedOfferKey + ", offerSignature="
                + c.offerSignature + ", offerHmac=" + c.offerHmac + ", encryptedAnswer="
                + c.encryptedAnswer + ", encryptedAnswerKey=" + c.encryptedAnswerKey
                + ", answerSignature=" + c.answerSignature + ", answerHmac=" + c.answerHmac
                + ", timeout=" + c.timeout + "]";
    }
}