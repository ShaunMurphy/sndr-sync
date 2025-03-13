package prototype;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import util.Config;
import util.Utilities;

import com.google.protobuf.Timestamp;
import com.sndr.proto.SndrFs;

public final class FileTransfer {
    
    /**
     * Generates a SndrFS message.
     * @param file - This can be a file or directory.
     * @return
     * @throws IOException
     */
    public static final SndrFs.SndrFS generateSndrFile(File file) throws IOException {
        SndrFs.SndrFS.Builder builder = SndrFs.SndrFS.newBuilder();
        builder.setName(file.getName());        
        if(file.isFile()) {
            builder.setType(SndrFs.SndrFS.DataType.File);
            SndrFs.MetaData.Builder metaData = SndrFs.MetaData.newBuilder();
            byte[] buffer = new byte[Config.DEFAULT_BUFFER_SIZE];
            metaData.setHash(Utilities.generateHash(file.toPath(), true, buffer));
            metaData.setMimetype("");
            metaData.setSize(file.length());
            
            Timestamp.Builder timestamp = Timestamp.newBuilder();
            timestamp.setSeconds(file.lastModified()/1000);
            metaData.setModified(timestamp);
            builder.setMetaData(metaData.build());            
        } else if(file.isDirectory()) {
            builder.setType(SndrFs.SndrFS.DataType.Directory);
        }
        return builder.build();
    }

    /**
     * 
     * @param input
     * @param output
     * @param size - The expected output size.
     */
    public static final void directCopy(final ReadableByteChannel input, final Path output, long size) {
        try(FileChannel channel = FileChannel.open(output, StandardOpenOption.WRITE, 
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);) {
            long count = size;
            long position = 0;
            long transferred = 0;
            while (count > 0) {
                transferred = channel.transferFrom(input, position, count);
                position += transferred;
                count -= transferred;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static final void directCopy(final Path input, final WritableByteChannel output, long size) {
        try (FileChannel channel = FileChannel.open(input, StandardOpenOption.READ);) {
            long count = size;
            long position = 0;
            long transferred = 0;
            while (count > 0) {
                transferred = channel.transferTo(position, count, output);
                position += transferred;
                count -= transferred;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
//    public static final void directCopy(final File input, final WritableByteChannel output, long size) {
//        try (FileChannel channel = FileChannel.open(input.toPath(), StandardOpenOption.READ);) {
//            long count = size;
//            long position = 0;
//            long transferred = 0;
//            while (count > 0) {
//                transferred = channel.transferTo(position, count, output);
//                position += transferred;
//                count -= transferred;
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
    
    
    
    
    
    /**
     * Copies a channel to another channel.
     * 
     * @param input
     * @param output
     * @throws IOException
     */
    public static final void fastCopy(final ReadableByteChannel input, final WritableByteChannel output, final ByteBuffer buffer) throws IOException {
        // Copy the channels
        while (input.read(buffer) != -1) {
            // Prepare the buffer to be drained
            buffer.flip();

            // Write to the channel, may block
            output.write(buffer);
            // If partial transfer, shift remainder down.
            // If buffer is empty, same as doing clear().
            buffer.compact();
        }

        // EOF will leave buffer in fill state.
        buffer.flip();
        // Make sure the buffer is fully drained.
        while (buffer.hasRemaining()) {
            output.write(buffer);
        }
        // Closing the channels
        // inputChannel.close();
        // outputChannel.close();
    }
}
