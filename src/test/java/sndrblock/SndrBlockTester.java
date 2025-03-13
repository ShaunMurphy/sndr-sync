package sndrblock;

import icelink.IceLink_Main;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import prototype.tasks.Call;
import prototype.tasks.Callback;
import server.SndrBlockServer;
import ssdp.message.SsdpConstants;
import util.Config;
import util.Config.ConnectionType;
import application.SSDP_Main;
import application.SndrBlockApi;
import application.interfaces.ApiHelper;
import application.interfaces.Crypto;
import application.interfaces.SyncMediator;
import client.SndrBlockClient;

import com.google.protobuf.GeneratedMessageV3;
import com.sndr.logger.SndrLogger;
import com.sndr.proto.SndrBlockProto.Request;
import com.sndr.proto.SndrBlockProto.RequestType;
import com.sndr.proto.SndrBlockProto.Response;
import common.ClientChannel;
import common.Serializer;
import common.connection.Connection;
import common.connection.SndrSyncConnection;

public class SndrBlockTester {
    private final Logger logger = SndrLogger.getLogger("ClientTester");
    //private final Crypto crypto;
    private final ApiHelper apiHelper;
    public final SndrBlockServer server;
    public final SndrBlockClient client;
    public final IceLink_Main icelink;
    public String account;

    private final SSDP_Main ssdpMain;
    private final SndrBlockApi block;

    
    public SndrBlockTester(final String account, final Crypto crypto, ApiHelper apiHelper, final SyncMediator syncMediator, String thisDeviceUUID, File rootDirectory) {
        Config.setRootDirectory(rootDirectory);

        this.account = account;
        //this.crypto = crypto;
        this.apiHelper = apiHelper;
        logger.log(Level.INFO, "This device UUID = "+thisDeviceUUID);
        Serializer.INSTANCE.initialize(crypto);
        this.icelink = new IceLink_Main();
        this.server = new SndrBlockServer(icelink, crypto, apiHelper);
        this.client = new SndrBlockClient(icelink, crypto, apiHelper);
        this.ssdpMain = new SSDP_Main(SsdpConstants.DEVICE_TYPES.DESKTOP, thisDeviceUUID, syncMediator);
        this.ssdpMain.start();
        
        this.block = new SndrBlockApi(client, apiHelper);
        URI uri = null;
        try {
            uri = new URI("mailto:testaccount@privategiant.com");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        this.block.setActiveURI(uri);
    }
    
    public final void setAccount(String account) {
        this.account = account;
    }
    

//    public static final File getDesktopRootDirectory() {
//        return new File(System.getProperty("user.home")+"/desktop");
//    }

    public final void startServer() {
        this.ssdpMain.setCreateConnectionHandler(this.server.getCreateConnectionHandler());
    }

    @Deprecated
    @SuppressWarnings("resource")
    private final void startClient1(final String remoteDeviceUuid) throws IOException {
        Connection connection = client.getConnectionManager().getConnection(ConnectionType.SNDR_SYNC, remoteDeviceUuid);
        if(connection == null) {
            client.getConnectionManager().createConnection(ConnectionType.SNDR_SYNC, remoteDeviceUuid);
            connection = client.getConnectionManager().getConnection(ConnectionType.SNDR_SYNC, remoteDeviceUuid);
        }

        System.out.println("Client - connection "+connection);
        
        if(connection == null) {
            System.out.println("Client - connection was null...");
            return;
        }
        
        //Create a request
        Request.ListDirectory.Builder request = Request.ListDirectory.newBuilder();
        request.setStartPath("/");
        request.setDepth(10);
        
        //For now...type cast the connection.
        ClientChannel channel = ((SndrSyncConnection)connection).getChannel();
        OutputStream output = channel.getOutputStream();
        try {
            Serializer.INSTANCE.writeDelimitedRequest(RequestType.LIST_DIRECTORY, request, output);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        InputStream input = channel.getInputStream();
        Response.ListDirectory response = Serializer.INSTANCE.<Response.ListDirectory>parseDelimitedResponse(RequestType.LIST_DIRECTORY, input);
        System.out.println("Client - response = "+response);
    } 

    @SuppressWarnings("unchecked")
    public final void startClient(final String remoteDeviceUuid, ConnectionType type, int test) {
        final Callback<? extends GeneratedMessageV3> standardCallback = new Callback<GeneratedMessageV3>() {
            @Override
            public void onResponse(Call<GeneratedMessageV3> call, prototype.Response<GeneratedMessageV3> response) {
                logger.log(Level.INFO, "onResponse() "+response.getData().toString());
            }

            @Override
            public void onFailure(Call<GeneratedMessageV3> call, Throwable t) {
                logger.log(Level.SEVERE, "onFailure()", t);
            }
        };
        
        switch (test) {
        case 1://Key Request
            Call<Response.Keys> call1 = block.getKeys(type, account, remoteDeviceUuid);
            call1.enqueue((Callback<Response.Keys>) standardCallback);
            break;
        case 2://List File Directory Request
            Call<Response.ListDirectory> call2 = block.getDirectoryTree(type, remoteDeviceUuid, "/", 10);
            call2.enqueue((Callback<Response.ListDirectory>) standardCallback);
            break;
        case 3://Upload File Request
            File file = new File("C:/Users/shaun/colors/14.jpg");
            //File file = new File("C:/Users/shaun/colors/video.mp4");
            Call<Response.UploadFile> call3 = block.uploadFile(remoteDeviceUuid, file);
            call3.enqueue((Callback<Response.UploadFile>) standardCallback);
            break;
        default:
            break;
        }
    }
    
    public final Response.Keys requestUserKeyPair(ConnectionType type, String remoteDeviceUuid) {
        Call<Response.Keys> call = block.getKeys(type, account, remoteDeviceUuid);
        prototype.Response<Response.Keys> response = null;
        try {
            response = call.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return response.getData();
    }
}
