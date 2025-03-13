package client.connection;

import fm.icelink.SessionDescription;
import icelink.IceLink_Main;
import icelink.RemoteConnection;
import icelink.RemoteConnection.ConnectionListener;
import icelink.RemoteConnection.ConnectionState;
import icelink.model.SDP;

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
    private final ApiHelper helper;
    private final Crypto crypto;
    private final IceLink_Main icelink;
    /*
    private enum State {
        READY, CREATE_OFFER, ENCRYPT_OFFER, CREATE_CONNECTION, WAIT_FOR_PUSH_NOTIFICATION, 
        GET_CONNECTION, PARSE_ANSWER, DONE, FAILED
    }*/
    
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
    public Connection createConnection(ConnectionManager manager, String remoteDeviceUuid) {
        IceLinkConnection connection = new IceLinkConnection(manager, remoteDeviceUuid);
        connection.setState(State.CONNECTING);
        SDP sdp = new SDP();
        sdp.remoteDeviceUuid = remoteDeviceUuid;
        createOffer(connection, sdp);
        encryptOffer(sdp.offer());
        helper.putOffer(sdp);
        if(sdp.connectionUuid == null) {
            System.out.println("IceLinkConnectionFactory - connection UUID is null");
            connection.setState(State.FAILED);
            return connection;
        }
        
        connection.getRemoteConnection().setConnectionId(sdp.connectionUuid);
        System.out.println("IceLinkConnectionFactory - Connection UUID is "+sdp.connectionUuid);
        //BLOCKING!
        this.icelink.lockThreadUntilNotify(connection);
        //TODO this needs to check to make sure the answer was ready.
        helper.getAnswer(sdp);
        parseAnswer(connection, sdp.answer());
        connection.getRemoteConnection().setAnswerJson(sdp.answer().json);
        
        //Wait for the connection state to become CONNECTED.
        final CountDownLatch latch = new CountDownLatch(1);
        connection.getRemoteConnection().setConnectionStateListener(new RemoteConnection.ConnectionStateListener() {
            @Override
            public void connectionStateChanged(ConnectionState state) {
                if(state.equals(RemoteConnection.ConnectionState.Connected)) {
                    latch.countDown();
                }
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        connection.getRemoteConnection().setConnectionStateListener(null);
        if(connection.getRemoteConnection().getConnectionState().equals(RemoteConnection.ConnectionState.Connected)) {
            connection.setState(State.CONNECTED);
        } else {
            connection.setState(State.FAILED);//??
        }
        
        
        return connection;
    }

    private final void createOffer(final IceLinkConnection connection, final SDP sdp) {
        final CountDownLatch latch = new CountDownLatch(1);
        connection.getRemoteConnection().createOffer(new ConnectionListener() {
            @Override
            public void offerCreated(SessionDescription offer) {
                //System.out.println(offer.getSdpMessage());
                sdp.offer().json = offer.toJson();
                latch.countDown();
            }
            @Override
            public void answerCreated(SessionDescription answer) {
                //Do nothing.
            }
        });
        //TODO Timeout...
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private final void encryptOffer(final SDP.Offer offer) {
        SecretKeySpec aesKey = crypto.generateRandomAesKey();
        KeyPair userKeys = crypto.getUserKeys(null);//Fix this
        offer.encryptedKey = crypto.protectAESKey(aesKey, userKeys.getPublic());
        byte[] json = Utilities.compress(offer.json);
        offer.encryptedJson = crypto.encrypt(aesKey, json);
        offer.hmac = crypto.generateHMAC(offer.encryptedJson, aesKey);
        offer.signature = crypto.signStringBase64(userKeys.getPrivate(), offer.encryptedJson);
        //connection.state = State.CREATE_CONNECTION;
    }

    private final void parseAnswer(final IceLinkConnection connection, final SDP.Answer answer) {
        KeyPair userKeys = crypto.getUserKeys(null);//Fix this
        SecretKeySpec aesKey = crypto.unprotectAESKey(answer.encryptedKey, userKeys.getPrivate());
        byte[] encryptedAnswerJson = answer.encryptedJson;
        byte[] compressedAnswer = crypto.decrypt(aesKey, encryptedAnswerJson);
        answer.json = Utilities.decompress(compressedAnswer);
        //connection.getRemoteConnection().setAnswerJson(answerJson);
        //TODO Validate HMAC and signature.
        boolean valid = crypto.verifyHMAC(answer.encryptedJson, answer.hmac, aesKey);
        //System.out.println("Valid answer that was parsed? "+valid);
    }
}