package icelink;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import fm.icelink.DataBuffer;
import fm.icelink.DataChannel;
import fm.icelink.DataChannelReceiveArgs;
import fm.icelink.IAction1;

public final class IceLinkInputStream extends InputStream implements AutoCloseable, IAction1<DataChannelReceiveArgs> {
    private final PipedOutputStream pos = new PipedOutputStream();
    private final PipedInputStream pis;
    private final DataChannel channel;

    public IceLinkInputStream(DataChannel channel) {
        this.channel = channel;
        channel.setOnReceive(this);
        PipedInputStream pis = null;
        try {
            pis = new PipedInputStream(pos, 512);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.pis = pis;
    }

    @Override
    public final void invoke(DataChannelReceiveArgs param) {
        DataBuffer buffer = param.getDataBytes();
        if(buffer == null) {
            throw new IllegalStateException("The buffer is null, this class should only be used for binary data.");
        }
        try {
            byte[] data = buffer.getData();
            pos.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public final synchronized int read() throws IOException {
        int i = pis.read();
        return i;
    }

    @Override
    public final int read(byte[] bytes) throws IOException {
        return pis.read(bytes);
    }

    @Override
    public final int read(byte[] bytes, int offset, int length) throws IOException {
        return pis.read(bytes, offset, length);
    }

   
    @Override
    public final long skip(long length) throws IOException {
        return pis.skip(length);
    }

    @Override
    public int available() throws IOException {
        return pis.available();
    }

    @Override
    public synchronized void mark(int readlimit) {
        throw new UnsupportedOperationException("Mark and reset are not supported");
    }

    @Override
    public synchronized void reset() {
        throw new UnsupportedOperationException("Mark and reset are not supported");
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public final void close() throws IOException {
        super.close();
        if(channel != null) {
            channel.setOnReceive(null);
        }
    }
}