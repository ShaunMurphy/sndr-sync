package application.interfaces;

import icelink.model.SDP;

import java.io.File;
import java.util.List;

import com.sndr.proto.SndrBlockProto.Request;
import com.sndr.proto.SndrBlockProto.Response;

public interface ApiHelper {
    
    public Response.SendMessage sendMessage(Request.SendMessage message);
    public Response.StashNote stashNote(Request.StashNote note);
    public Response.StashFile stashFile(Request.StashFile file);
    public boolean isDeviceUuidValid(String deviceUuid);
    public List<File> generateThumbnails(List<File> file);

    public Request.Keys.Builder generateKeyRequest(String account);
    public Response.Keys.Builder generateKeyResponse(Request.Keys request);
    public void getOffer(SDP sdp);
    public void putOffer(SDP sdp);
    public void getAnswer(SDP sdp);
    public void putAnswer(SDP sdp);
}