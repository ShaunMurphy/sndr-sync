package prototype;

import java.io.File;
import java.net.URI;
import java.util.UUID;

import util.Config.ConnectionType;

import com.google.protobuf.GeneratedMessageV3;
import com.sndr.proto.SndrBlockProto.RequestType;

public class Request {
    public URI userUri;
    public ConnectionType connectionType;
    public String deviceUuid;

    public RequestType type;
    public GeneratedMessageV3.Builder<?> messageBuilder;
    public File file;
}