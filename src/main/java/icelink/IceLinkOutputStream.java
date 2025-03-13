package icelink;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import fm.icelink.DataBuffer;
import fm.icelink.DataChannel;

public final class IceLinkOutputStream extends OutputStream implements AutoCloseable {
    private final DataChannel channel;

    public IceLinkOutputStream(DataChannel channel) {
        this.channel = channel;
    }

    @Override
    public final void write(int b) throws IOException {
        //pos.write(b);
        //DataBuffer bytes = DataBuffer.wrap();
        //channel.sendDataBytes(bytes);
    }
    
    @Override
    public final void write(byte[] bytes) throws IOException {
        //pos.write(bytes);
    }

    @Override
    public final void write(byte[] bytes, int offset, int length) throws IOException {
        //pos.write(bytes, offset, length);
    }

    @Override
    public final void flush() throws IOException {
        //pos.flush();
    }

    @Override
    public final void close() throws IOException {
        super.close();
        if(channel != null) {
            channel.setOnReceive(null);
        }
    }
}