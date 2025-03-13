package common;

import fm.icelink.DataBuffer;
import icelink.RemoteConnection;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import prototype.FileTransfer;
import util.Config;
import application.interfaces.Crypto;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.sndr.logger.SndrLogger;
import com.sndr.proto.SndrBlockProto;
import com.sndr.proto.SndrBlockProto.Authentication;
import com.sndr.proto.SndrBlockProto.Request;
import com.sndr.proto.SndrBlockProto.RequestType;
import com.sndr.proto.SndrBlockProto.Response;
import com.sndr.proto.SndrFs;

/**
 * A serializing and deserializing class. 
 * This is used to serialize protobuf objects to OutputStreams and deserialize protobuf objects from InputStreams.
 * @author shaun
 */
public enum Serializer {
    INSTANCE;
    private static final Logger logger = SndrLogger.getLogger();
    private Crypto crypto;
    
    public final void initialize(Crypto crypto) {
        this.crypto = crypto;
    }

    private final Authentication generateAuthentication(GeneratedMessageV3.Builder<?> builder, FieldDescriptor descriptor) {
        RequestType type = null;
        //For Request Builders.
        if(builder instanceof Request.ListDirectory.Builder) {
            type = RequestType.LIST_DIRECTORY;
        } else if(builder instanceof Request.UploadFile.Builder) {
            type = RequestType.UPLOAD_FILE;
        } else if(builder instanceof Request.DownloadFile.Builder) {
            type = RequestType.DOWNLOAD_FILE;
        } else if(builder instanceof Request.DeleteFile.Builder) {
            type = RequestType.DELETE_FILE;
        } else if(builder instanceof Request.MoveFile.Builder) {
            type = RequestType.MOVE_FILE;
        } else if(builder instanceof Request.SendMessage.Builder) {
            type = RequestType.SEND_MESSAGE;
        } else if(builder instanceof Request.StashNote.Builder) {
            type = RequestType.STASH_NOTE;
        } else if(builder instanceof Request.StashFile.Builder) {
            type = RequestType.STASH_FILE;
        } else if(builder instanceof Request.Keys.Builder) {
            type = RequestType.KEYS;
        }
        //For Response Builders
        else if(builder instanceof Response.ListDirectory.Builder) {
            type = RequestType.LIST_DIRECTORY;
        } else if(builder instanceof Response.UploadFile.Builder) {
            type = RequestType.UPLOAD_FILE;
        } else if(builder instanceof Response.DownloadFile.Builder) {
            type = RequestType.DOWNLOAD_FILE;
        } else if(builder instanceof Response.DeleteFile.Builder) {
            type = RequestType.DELETE_FILE;
        } else if(builder instanceof Response.MoveFile.Builder) {
            type = RequestType.MOVE_FILE;
        } else if(builder instanceof Response.SendMessage.Builder) {
            type = RequestType.SEND_MESSAGE;
        } else if(builder instanceof Response.StashNote.Builder) {
            type = RequestType.STASH_NOTE;
        } else if(builder instanceof Response.StashFile.Builder) {
            type = RequestType.STASH_FILE;
        } else if(builder instanceof Response.Keys.Builder) {
            type = RequestType.KEYS;
        }
        //For everything else.
        else {
            type = RequestType.UNRECOGNIZED;
        }

        Authentication.Builder authentication = Authentication.newBuilder();
        authentication.setType(type);
        authentication.setUserUri("userURI");
        authentication.setTimestamp(getTimestamp());
        //Add the authentication to the protobuf before generating the signature.
        builder.setField(descriptor, authentication.buildPartial());

        authentication.setSignature(generateSignature(builder));
        return authentication.build();
    }
    //Returns the date time now.
    private final Timestamp getTimestamp() {
        Timestamp.Builder timestamp = Timestamp.newBuilder();
        long now = new Date().getTime()/1000;
        timestamp.setSeconds(now);
        return timestamp.build();
    }
    //Make sure to call this last.
    /**
     * Generates a signature. If you called {@link #generateAuthentication(builder)} 
     * then there is no need to call this method since that method calls this one.
     * @param builder
     * @return
     */
    private final ByteString generateSignature(GeneratedMessageV3.Builder<?> builder) {
        byte[] signThis = builder.buildPartial().toByteArray();
        byte[] signature = crypto.signStringBase64(signThis);
        return ByteString.copyFrom(signature);
    }

    /**
     * Generates an Authentication message then adds it to the given protobuf object.
     * @param builder
     */
    private final void setAuthenticationField(final GeneratedMessageV3.Builder<?> builder) {
        //Adds the authentication to the protobuf object.
        FieldDescriptor descriptor = builder.getDescriptorForType().findFieldByName("authentication");
        if(descriptor != null) {
            Authentication authentication = generateAuthentication(builder, descriptor);
            builder.setField(descriptor, authentication);
        } else {
            logger.log(Level.SEVERE, "Could not find the field \"authentication\" on the protobuf object "
                    +builder.getClass().getName());
        }
    }

    /**
     * Validates the Authentication message for the given protobuf object.
     * @param protobuf
     * @return - true if valid, false otherwise.
     */
    public final boolean validateAuthentication(final GeneratedMessageV3 protobuf) {
        if(protobuf == null || protobuf.getSerializedSize() == 0) {
            logger.log(Level.WARNING, "Cannot validate the protofuf, it was null or empty.");
            return false;
        }
        FieldDescriptor descriptor = protobuf.getDescriptorForType().findFieldByName("authentication");
        if (descriptor == null) {
            logger.log(Level.SEVERE, "Could not find the field \"authentication\" on the protobuf "
                    + protobuf.getClass().getName());
            return false;
        }

        Authentication authentication = (Authentication) protobuf.getField(descriptor);
        final byte[] signature = authentication.getSignature().toByteArray();
        //Remove the signature from the authentication object and add it to the protobuf object.
        Authentication authenticationPartial = authentication.toBuilder().clearSignature().buildPartial();
        Message protobufNoSignature = protobuf.toBuilder().setField(descriptor, authenticationPartial).buildPartial();
        final byte[] input = protobufNoSignature.toByteArray();
        
        //Some how the device UUID is needed.
        String deviceUuid = null;//authentication.getDeviceUuid();
        
        return crypto.validateSignature(deviceUuid, signature, input);
    }

    /**
     * Serializes the protobuf object onto the output stream. Use this for requests.<br>
     * This will attempt to add the authentication object to the protobuf object.<br>
     * The protobuf object must have a field called "authentication".
     * @param type
     * @param requestBuilder
     * @param output
     * @throws IOException
     */
    public final void writeDelimitedRequest(final RequestType type, final GeneratedMessageV3.Builder<?> requestBuilder, final OutputStream output) throws IOException {
        if(requestBuilder == null) {
            logger.log(Level.SEVERE, "Did not send a request to the server!");
            return;
        }
        setAuthenticationField(requestBuilder);
        GeneratedMessageV3 request = (GeneratedMessageV3) requestBuilder.build();
        // Write how many items the server should expect.
        SndrBlockProto.Header.Builder header = SndrBlockProto.Header.newBuilder();
        header.setType(type);
        header.setQuantity(1);
        header.build().writeDelimitedTo(output);
        request.writeDelimitedTo(output);
    }

    /**
     * Serializes the protobuf object onto the output stream. Use this for responses.<br>
     * This will attempt to add the authentication object to the protobuf object.<br>
     * The protobuf object must have a field called "authentication".
     * @param type
     * @param responseBuilder
     * @param output
     * @throws IOException
     */
    public final void writeDelimitedResponse(final RequestType type, final GeneratedMessageV3.Builder<?> responseBuilder, final OutputStream output) throws IOException {
        if (responseBuilder == null) {
            logger.log(Level.SEVERE, "Did not send a response to the client!");
            return;
        }
        setAuthenticationField(responseBuilder);
        GeneratedMessageV3 response = (GeneratedMessageV3) responseBuilder.build();
        response.writeDelimitedTo(output);
    }

    @SuppressWarnings("unchecked")
    public final <Data extends GeneratedMessageV3> Data parseDelimitedRequest(final RequestType type, final InputStream input) {
        try {
            switch(type) {
            case LIST_DIRECTORY:
                return (Data) Request.ListDirectory.parseDelimitedFrom(input);
            case DOWNLOAD_FILE:
                return (Data) Request.DownloadFile.parseDelimitedFrom(input);
            case UPLOAD_FILE:
                return (Data) Request.UploadFile.parseDelimitedFrom(input);
            case MOVE_FILE:
                return (Data) Request.MoveFile.parseDelimitedFrom(input);
            case DELETE_FILE:
                return (Data) Request.DeleteFile.parseDelimitedFrom(input);
            case SEND_MESSAGE:
                return (Data) Request.SendMessage.parseDelimitedFrom(input);
            case STASH_NOTE:
                return (Data) Request.StashNote.parseDelimitedFrom(input);
            case STASH_FILE:
                return (Data) Request.StashFile.parseDelimitedFrom(input);
            case KEYS:
                return (Data) Request.Keys.parseDelimitedFrom(input);
            case UNRECOGNIZED:
            default:
                throw new UnsupportedOperationException("The request type "+type+" is not implemented.");
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "", e);
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public final <Data extends GeneratedMessageV3> Data parseDelimitedResponse(final RequestType type, final InputStream input) throws IOException {
        switch(type) {
        case LIST_DIRECTORY:
            return (Data) Response.ListDirectory.parseDelimitedFrom(input);
        case DOWNLOAD_FILE:
            return (Data) Response.DownloadFile.parseDelimitedFrom(input);
        case UPLOAD_FILE:
            return (Data) Response.UploadFile.parseDelimitedFrom(input);
        case MOVE_FILE:
            return (Data) Response.MoveFile.parseDelimitedFrom(input);
        case DELETE_FILE:
            return (Data) Response.DeleteFile.parseDelimitedFrom(input);
        case SEND_MESSAGE:
            return (Data) Response.SendMessage.parseDelimitedFrom(input);
        case STASH_NOTE:
            return (Data) Response.StashNote.parseDelimitedFrom(input);
        case STASH_FILE:
            return (Data) Response.StashFile.parseDelimitedFrom(input);
        case KEYS:
            return (Data) Response.Keys.parseDelimitedFrom(input);
        case UNRECOGNIZED:
        default:
            throw new UnsupportedOperationException("The request type "+type+" is not implemented.");
        }
    }

    public final void writeDelimitedRequest(final RequestType type, final GeneratedMessageV3.Builder<?> requestBuilder, final RemoteConnection remoteConnection) {
        if(requestBuilder == null) {
            logger.log(Level.SEVERE, "Did not send a request to the server!");
            return;
        }
        setAuthenticationField(requestBuilder);
        GeneratedMessageV3 request = (GeneratedMessageV3) requestBuilder.build();
        // Write how many items the server should expect.
        SndrBlockProto.Header.Builder headerBuilder = SndrBlockProto.Header.newBuilder();
        headerBuilder.setType(type);
        headerBuilder.setQuantity(1);
        SndrBlockProto.Header header = headerBuilder.build();
        //header.build().writeDelimitedTo(output);
        //request.writeDelimitedTo(output);
        System.out.println("------- IceLink request -----------------");
        //icelinkWrite(type, request, remoteConnection, true);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream(request.getSerializedSize()+header.getSerializedSize());
        try {
            header.writeDelimitedTo(baos);
            request.writeDelimitedTo(baos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        DataBuffer newBuffer = DataBuffer.wrap(baos.toByteArray());
        remoteConnection.getProtobufChannel().sendDataBytes(newBuffer);        
    }

    public final void writeDelimitedResponse(final RequestType type, final GeneratedMessageV3.Builder<?> responseBuilder, final RemoteConnection remoteConnection) {
        if (responseBuilder == null) {
            logger.log(Level.SEVERE, "Did not send a response to the client!");
            return;
        }
        setAuthenticationField(responseBuilder);
        GeneratedMessageV3 response = (GeneratedMessageV3) responseBuilder.build();
        //response.writeDelimitedTo(output);
        System.out.println("------- IceLink response -----------------");
        

        ByteArrayOutputStream baos = new ByteArrayOutputStream(response.getSerializedSize());
        try {
            response.writeDelimitedTo(baos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        DataBuffer newBuffer = DataBuffer.wrap(baos.toByteArray());
        remoteConnection.getProtobufChannel().sendDataBytes(newBuffer);
        
        
        //icelinkWrite(type, response, remoteConnection, false);
    }

    @Deprecated
    private final void icelinkWrite(final RequestType type, final GeneratedMessageV3 protobuf, final RemoteConnection connection, boolean isRequest) {
        byte[] protobufData = protobuf.toByteArray();
        DataBuffer buffer = DataBuffer.allocate(protobufData.length+12);
        //Write the custom header.
        buffer.write32(type.getNumber(), 0);
        buffer.write32(protobuf.getSerializedSize(), 4);
        buffer.write32(isRequest ? 0 : 1, 8);
        buffer.writeBytes(protobufData, 0, protobufData.length, 12);
        //connection.updateLastContacted();
        //System.out.println("Ice link write - about to send protobuf databytes buffer "+Thread.currentThread().getName());
        try {
            connection.getProtobufChannel().sendDataBytes(buffer);
        } catch(Exception e) {
            System.out.println("Something broke....");
            e.printStackTrace();
        }
        
        //System.out.println("Ice link write finished");
//        
//        ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
//        try {
//            protobuf.writeDelimitedTo(baos);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        
//        DataBuffer newBuffer = DataBuffer.wrap(baos.toByteArray());
//        connection.getProtobufChannel().sendDataBytes(newBuffer);
    }

    @SuppressWarnings("unchecked")
    public final <Data extends GeneratedMessageV3> Data parseRequest(final RequestType type, final InputStream input) {
        try {
            switch(type) {
            case LIST_DIRECTORY:
                return (Data) Request.ListDirectory.parseFrom(input);
            case DOWNLOAD_FILE:
                return (Data) Request.DownloadFile.parseFrom(input);
            case UPLOAD_FILE:
                return (Data) Request.UploadFile.parseFrom(input);
            case MOVE_FILE:
                return (Data) Request.MoveFile.parseFrom(input);
            case DELETE_FILE:
                return (Data) Request.DeleteFile.parseFrom(input);
            case SEND_MESSAGE:
                return (Data) Request.SendMessage.parseFrom(input);
            case STASH_NOTE:
                return (Data) Request.StashNote.parseFrom(input);
            case STASH_FILE:
                return (Data) Request.StashFile.parseFrom(input);
            case KEYS:
                return (Data) Request.Keys.parseFrom(input);
            case UNRECOGNIZED:
            default:
                throw new UnsupportedOperationException("The request type "+type+" is not implemented.");
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "", e);
        }
        return null;
    }

    //Needs testing!
    //public final void parseDelimitedResponse(final RequestType type, RemoteConnection remoteConnection) {
    //}
    
    
    //TODO Add the one for remote requests.
    //Its found in SndrBlockService.java  sendProfobuf(RemoteConnection ...)

    
    
    public final void sendFile(final String path, File file, final ClientChannel channel) throws IOException {
        //Prepare the file.

        Request.UploadFile.Builder builder = Request.UploadFile.newBuilder();
        builder.setPath(path);
        SndrFs.SndrFS sndrFile = FileTransfer.generateSndrFile(file);
        builder.setFile(sndrFile);
        final RequestType type = RequestType.UPLOAD_FILE;
        
        /*
        @SuppressWarnings("resource")
        final OutputStream output = channel.getOutputStream();
        // Write how many items the server should expect.
        Header.Builder header = SndrBlockProto.Header.newBuilder();
        header.setType(type);
        header.setQuantity(1);
        header.build().writeDelimitedTo(output);

        //Send the request.
        setAuthenticationField(builder);
        GeneratedMessageV3 request = (GeneratedMessageV3) builder.build();
        request.writeDelimitedTo(output);*/
        writeDelimitedRequest(type, builder, channel.getOutputStream());
        
        //Send the file.
        Path inputPath = file.toPath();
        long size = builder.getFile().getMetaData().getSize();
        FileTransfer.directCopy(inputPath, channel.getClientChannel(), size);
        
        
    }
    
    public final void receiveFile(final Request.UploadFile request, final ClientChannel channel) throws IOException {
        SndrFs.SndrFS file = request.getFile();
        File saveAs = Config.resolveFile(request.getPath(), file.getName());
        Path outputPath = saveAs.toPath();
        long size = file.getMetaData().getSize();
        // long hash = file.getMetaData().getHash();
        FileTransfer.directCopy(channel.getClientChannel(), outputPath, size);       
    }
    
    
    
    /*
    public final void sendFile(final String path, File file, RemoteConnection remoteConnection) throws IOException {
        Request.UploadFile.Builder builder = Request.UploadFile.newBuilder();
        builder.setPath(path);
        SndrFs.SndrFS sndrFile = FileTransfer.generateSndrFile(file);
        builder.setFile(sndrFile);
        final RequestType type = RequestType.UPLOAD_FILE;
        
        writeDelimitedRequest(type, builder, remoteConnection);
        
        //Send the file.
        Path inputPath = file.toPath();
        long size = builder.getFile().getMetaData().getSize();
        //FileTransfer.directCopy(inputPath, channel.getClientChannel(), size);
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        
        
        
        //FileTransfer.fastCopy(input, output, buffer);
        
        
    }
    */
    
    
    
    
    
    
    /*
    public final void sendFiles(final String path, List<File> files, final ClientChannel channel) throws IOException {
        //Prepare the files.
        final List<Request.UploadFile.Builder> filesToSend = new ArrayList<>(files.size());
        for(File file : files) {
            Request.UploadFile.Builder builder = Request.UploadFile.newBuilder();
            builder.setPath(path);
            SndrFs.SndrFS sndrFile = FileTransfer.generateSndrFile(file);
            builder.setFile(sndrFile);
            filesToSend.add(builder);
        }

        final RequestType type = RequestType.UPLOAD_FILE;
        @SuppressWarnings("resource")
        final OutputStream output = channel.getOutputStream();
        // Write how many items the server should expect.
        Header.Builder header = SndrBlockProto.Header.newBuilder();
        header.setType(type);
        header.setQuantity(filesToSend.size());
        header.build().writeDelimitedTo(output);

        for(int i=0; i<files.size(); i++) {
            Request.UploadFile.Builder requestBuilder = filesToSend.get(i);
            //Send the request.
            setAuthenticationField(requestBuilder);
            GeneratedMessageV3 request = (GeneratedMessageV3) requestBuilder.build();
            request.writeDelimitedTo(output);
            //Send the file.
            Path inputPath = files.get(i).toPath();
            long size = requestBuilder.getFile().getMetaData().getSize();
            FileTransfer.directCopy(inputPath, channel.getClientChannel(), size);
        }
    }
    
    public final void receiveFiles(SndrBlockProto.Header header, final ClientChannel channel) throws IOException {
        final RequestType type = RequestType.UPLOAD_FILE;
        @SuppressWarnings("resource")
        final InputStream input = channel.getInputStream();
        @SuppressWarnings("resource")
        final OutputStream output = channel.getOutputStream();
        
        final int max = header.getQuantity();
        for(int i=0; i<max; i++) {
            //Receive the upload file request.
            Request.UploadFile request = parseDelimitedRequest(type, input);
            //TODO Validate authentication.
            
            SndrFs.SndrFS file = request.getFile();
            File saveAs = Config.resolveFile(request.getPath(), file.getName());
            Path outputPath = saveAs.toPath();
            long size = file.getMetaData().getSize();
            //long hash = file.getMetaData().getHash();
            FileTransfer.directCopy(channel.getClientChannel(), outputPath, size);
            
            //Send response.
            Response.UploadFile.Builder response = Response.UploadFile.newBuilder();
            response.setStatus("success file #"+(i+1)+"/"+max);
            writeDelimitedResponse(type, response, output);
        }
    }*/
    
}