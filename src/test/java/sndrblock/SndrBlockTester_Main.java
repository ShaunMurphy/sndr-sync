package sndrblock;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import util.Config.ConnectionType;
import application.interfaces.ApiHelper;
import application.interfaces.Crypto;
import application.interfaces.SyncMediator;

import com.sndr.api.SndrApi;
import com.sndr.api.crypto.ApiCrypto;
import com.sndr.httpclient.util.Base64;
import com.sndr.logger.SndrLogger;

public final class SndrBlockTester_Main {
    private static SndrLogger logger = new SndrLogger.Builder(SndrBlockTester_Main.class.getName()).enableConsoleLogger().build();
    //Test Account's
    private static String michaelIPhone = "BCE85DA1-25D4-4E69-BD83-9121F000A242";
    private static String michaelIPad = "F59DF946-8144-4CDD-BCFE-78655FD21689";
    private static String shaunDeviceUuid = "17f5d206-6267-48f9-a5e6-0f7e7f007e0a";
    private static final String account = "ReliableOpenEndedRam";//Test account

    private static final ExecutorService executor = Executors.newFixedThreadPool(2, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        }
    });

    private static final Tester tester = Tester.INSTANCE;

    public static void main(String[] args) {        
        logger.setLevel(Level.ALL);
        final String thisDeviceUuid = tester.getDeviceId();
        Everything everything = new Everything(account, tester.getKeyManager());
        SyncMediator syncMediator = everything;
        Crypto crypto = everything;
        ApiHelper api = everything;
        initializeSndrApi(crypto);
        //Tester.INSTANCE.login();

        File root = tester.getDirectory();
        final SndrBlockTester sb = new SndrBlockTester(account, crypto, api, syncMediator, thisDeviceUuid, root);

        Runnable server = new Runnable() {
            @Override
            public final void run() {
                sb.startServer(); 
            }
        };

        Runnable client = new Runnable() {
            @Override
            public final void run() {
                //sb.startClient(shaunDeviceUuid, ConnectionType.SNDR_SYNC, 1);
                sb.startClient(thisDeviceUuid, ConnectionType.SNDR_SYNC, 2);
                //sb.startClient(thisDeviceUuid, ConnectionType.ICELINK, 2);                
            }
        };

        executor.execute(server);
        executor.execute(client);

        CountDownLatch latch = new CountDownLatch(1);
        System.out.println("Main thread blocked for 1 minute.");
        try {
            latch.await(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("The application died...");
    }

    private static final void initializeSndrApi(final Crypto crypto) {
        KeyStore sslKeyStore = null;
        URL hostURL = null;
        try {
            hostURL = new URL(Tester.HOST);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        
        ApiCrypto apiCrypto = new ApiCrypto() {
            @Override
            public String signWithDevicePrivateKey(String data) {
                //System.out.println("\n"+data);
                String signature = new String(crypto.signStringBase64(data.getBytes(Charset.forName("UTF-8"))), Charset.forName("UTF-8"));
                //PrivateKey privateKey = tester.getDevicePrivateKey();                
                //byte[] signatureBytes = crypto.signStringBase64(privateKey, data.getBytes(Charset.forName("UTF-8")));
                //String signature = new String(signatureBytes, Charset.forName("UTF-8"));
                return signature;
            }
            @Override
            public String hashStringToMD5Base64(String content) {
                
                return crypto.hashStringToMD5Base64(content);
            }
        };
        new SndrApi(sslKeyStore, logger, hostURL, apiCrypto, tester.getDeviceId(), "9", "JavaFx");
    }
}