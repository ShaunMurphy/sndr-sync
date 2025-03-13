package application;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import prototype.FileTransfer;
import prototype.tasks.Call;
import util.Config.ConnectionType;
import application.interfaces.ApiHelper;
import client.SndrBlockClient;

import com.sndr.proto.SndrBlockProto.Request;
import com.sndr.proto.SndrBlockProto.RequestType;
import com.sndr.proto.SndrBlockProto.Response;
import com.sndr.proto.SndrBlockProto.Response.UploadFile;
import com.sndr.proto.SndrFs;

public final class SndrBlockApi {
    private final SndrBlockClient client;
    private final ApiHelper apiHelper;
    private URI activeURI;

    public SndrBlockApi(final SndrBlockClient client, ApiHelper apiHelper) {
        this.client = client;
        this.apiHelper = apiHelper;
    }

    /**
     * Sets the active URI. This is used in request validations.
     * @param uri
     */
    public final void setActiveURI(URI uri) {
        this.activeURI = uri;
    }
    
    /**
     * Makes a request to get the user's keys from a remote device.
     * @param type
     * @param account
     * @param remoteDeviceUuid
     * @return
     */
    public final Call<Response.Keys> getKeys(ConnectionType type, final String account, final String remoteDeviceUuid) {
        Request.Keys.Builder builder = apiHelper.generateKeyRequest(account);
        prototype.Request request = new prototype.Request();
        request.deviceUuid = remoteDeviceUuid;
        request.connectionType = type;
        request.type = RequestType.KEYS;
        request.messageBuilder = builder;
        request.userUri = activeURI;
        Call<Response.Keys> call = this.client.sendRequest(request);
        return call;
    }
    
    /**
     * Makes a request to get the file system tree on a remote device.
     * @param type
     * @param remoteDeviceUuid
     * @param path
     * @param depth
     * @return
     */
    public final Call<Response.ListDirectory> getDirectoryTree(ConnectionType type, final String remoteDeviceUuid, String path, int depth) {
        Request.ListDirectory.Builder builder = Request.ListDirectory.newBuilder();
        builder.setStartPath(path);
        builder.setDepth(depth);

        prototype.Request request = new prototype.Request();
        request.deviceUuid = remoteDeviceUuid;
        request.connectionType = type;
        request.type = RequestType.LIST_DIRECTORY;
        request.messageBuilder = builder;
        request.userUri = activeURI;
        Call<Response.ListDirectory> call = this.client.sendRequest(request);
        return call;
    }

    /**
     * Makes a request to upload a file to a remote device.
     * @param remoteDeviceUuid
     * @param file
     * @return
     */
    public final Call<UploadFile> uploadFile(String remoteDeviceUuid, File file) {
        //Only local is supported for now.
        ConnectionType type = ConnectionType.SNDR_SYNC;

        Request.UploadFile.Builder builder = Request.UploadFile.newBuilder();
        builder.setPath("/");
        try {
            SndrFs.SndrFS sndrFile = FileTransfer.generateSndrFile(file);
            builder.setFile(sndrFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        prototype.Request request = new prototype.Request();
        request.deviceUuid = remoteDeviceUuid;
        request.connectionType = type;
        request.type = RequestType.UPLOAD_FILE;
        request.messageBuilder = builder;
        request.userUri = activeURI;
        
        request.file = file;
        Call<Response.UploadFile> call = this.client.sendRequest(request);
        return call;
    }
}