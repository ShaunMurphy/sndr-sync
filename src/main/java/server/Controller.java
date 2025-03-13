package server;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import server.annotations.Route;
import application.interfaces.ApiHelper;
import application.interfaces.Crypto;

import com.sndr.logger.SndrLogger;
import com.sndr.proto.SndrBlockProto;
import com.sndr.proto.SndrBlockProto.Request;
import com.sndr.proto.SndrBlockProto.Response;
import com.sndr.proto.SndrFs.SndrFS;
import common.ClientChannel;
import common.Serializer;

public final class Controller {
    private static final Logger logger = SndrLogger.getLogger();
    private static Crypto crypto;
    private static ApiHelper helper;
    public Controller(Crypto crypto, ApiHelper helper) {
        Controller.crypto = crypto;
        Controller.helper = helper;
    }

    @Route(type = SndrBlockProto.RequestType.KEYS)
    public static final Response.Keys.Builder syncKeys(Request.Keys request) {
        logger.log(Level.INFO, "Keys - {0}", request.getUsername());
        Response.Keys.Builder response = helper.generateKeyResponse(request);
        return response;
    }

    @Route(type = SndrBlockProto.RequestType.LIST_DIRECTORY)
    public static final Response.ListDirectory.Builder listDirectory(Request.ListDirectory request) {
        logger.log(Level.INFO, "List Directory - {0} {1}", new Object[]{request.getStartPath(), request.getDepth()});
        //System.out.println(request.getAuthentication());

        String path = request.getStartPath();
        int depth = request.getDepth();
        //File directory = Config.resolveDirectory(path);
        //SndrFS root = Database.get().getProtobufData(directory, depth);
        
        //This is temporary until this is fixed for Android.
        SndrFS.Builder build = SndrFS.newBuilder();
        build.setName("Root directory (this is temporary)");
        SndrFS root = build.build();
        
        if(root == null) {
            root = SndrFS.newBuilder().build();
        }

        Response.ListDirectory.Builder response = Response.ListDirectory.newBuilder();
        response.setStartPath(path);
        response.setDepth(depth);
        response.setRoot(root);
        return response;
    }
    
    @Route(type = SndrBlockProto.RequestType.UPLOAD_FILE)
    public static final Response.UploadFile.Builder uploadFile(Request.UploadFile request, ClientChannel channel) {
        logger.log(Level.INFO, "UploadFile "+request.getPath());
        //System.out.println("Controller - UploadFile - "+request.getPath()+" "+request.getFile());
        long start = System.currentTimeMillis();
        long size = request.getFile().getMetaData().getSize();
        try {
            Serializer.INSTANCE.receiveFile(request, channel);
        } catch (IOException e) {
            e.printStackTrace();
        }

        long duration = System.currentTimeMillis() - start;
        double rate = (size*0.000953674)/duration;//MB/s
        // Send response.
        Response.UploadFile.Builder response = Response.UploadFile.newBuilder();
        response.setStatus("Success! File received in "+duration+" ms at "+rate+" MB/s.");
        return response;
    }
    
    
    
}