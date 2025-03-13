package server.connection;

import fm.icelink.SessionDescription;
import icelink.IceLink_Main;
import icelink.RemoteConnection.ConnectionListener;
import icelink.model.SDP;

import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.util.concurrent.CountDownLatch;

import javax.crypto.spec.SecretKeySpec;

import util.Config.ConnectionType;
import util.Utilities;
import application.interfaces.ApiHelper;
import application.interfaces.Crypto;

import common.connection.Connection;
import common.connection.Connection.State;
import common.connection.IceLinkConnection;

public final class IceLinkConnectionFactory implements ConnectionFactory {
    private ApiHelper helper;
    private final Crypto crypto;
    private final IceLink_Main icelink;

    public IceLinkConnectionFactory(IceLink_Main icelink, ApiHelper helper, Crypto crypto) {
        this.icelink = icelink;
        this.helper = helper;
        this.crypto = crypto;
    }

    @Override
    public ConnectionType getSupportedConnectionType() {
        return ConnectionType.ICELINK;
    }

    @Override
    public Connection createConnection(ConnectionManager manager, String remoteDeviceUuid, InetSocketAddress remoteSocketAddress) {
        throw new UnsupportedOperationException("Not supported");
    }
    
    @Override
    public Connection createConnection(ConnectionManager manager, String remoteDeviceUuid, String connectionUuid) {
        //A push notification handler will cause this connection to be created.        
        IceLinkConnection connection = new IceLinkConnection(null, remoteDeviceUuid);
        connection.setState(State.CONNECTING);
        SDP sdp = new SDP();
        sdp.connectionUuid = connectionUuid;
        sdp.remoteDeviceUuid = remoteDeviceUuid;
        
        helper.getOffer(sdp);//get the offer
        parseOffer(sdp.offer());
        //Set the offer to the RemoteConnection.
        connection.getRemoteConnection().setOfferJson(sdp.offer().json);
        createAnswer(connection, sdp);
        encryptAnswer(sdp.answer());
        helper.putAnswer(sdp);
        return connection;
    }

    private final void parseOffer(final SDP.Offer offer) {
        KeyPair userKeys = crypto.getUserKeys("");
        SecretKeySpec aesKey = crypto.unprotectAESKey(offer.encryptedKey, userKeys.getPrivate());
        byte[] encryptedOfferJson = offer.encryptedJson;
        byte[] compressedOffer = crypto.decrypt(aesKey, encryptedOfferJson);
        offer.json = Utilities.decompress(compressedOffer);
        //TODO Validate HMAC and signature.
        
        boolean valid = crypto.verifyHMAC(offer.encryptedJson, offer.hmac, aesKey);
        //System.out.println("Valid offer that was parsed? "+valid);
        //TODO Check timeout
    }
    
    private final void createAnswer(final IceLinkConnection connection, final SDP sdp) {
        final CountDownLatch latch = new CountDownLatch(1);
        connection.getRemoteConnection().createAnswer(new ConnectionListener() {
            @Override
            public void offerCreated(SessionDescription offer) {
                //Do nothing
            }

            @Override
            public void answerCreated(SessionDescription answer) {
                sdp.answer().json = answer.toJson();
                //sdp.state = State.ENCRYPT_ANSWER;
                latch.countDown();
            }
        });
        //TODO Timeout...
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private final void encryptAnswer(final SDP.Answer answer) {
        KeyPair userKeys = crypto.getUserKeys("");
        SecretKeySpec aesKey = crypto.generateRandomAesKey();
        answer.encryptedKey = crypto.protectAESKey(aesKey, userKeys.getPublic());
        byte[] json = Utilities.compress(answer.json);
        answer.encryptedJson = crypto.encrypt(aesKey, json);
        answer.hmac = crypto.generateHMAC(answer.encryptedJson, aesKey);
        answer.signature = crypto.signStringBase64(userKeys.getPrivate(), answer.encryptedJson); 
        //connection.state = State.UPDATE_CONNECTION;
    }

}