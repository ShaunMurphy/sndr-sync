package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import net.jpountz.xxhash.StreamingXXHash32;
import net.jpountz.xxhash.StreamingXXHash64;
import net.jpountz.xxhash.XXHashFactory;

public final class Utilities {
    private static final Deflater deflater = new Deflater(5, true);
    private static final Inflater inflater = new Inflater(true);
    private static final Object tempLock = new Object();
    private static byte[] temp = new byte[4*1024];

    
    private Utilities() {}
    
    
    //TODO Optimize!
    public static final byte[] compress(String input) {
        byte[] data = null;
        synchronized (tempLock) {
            deflater.setInput(input.getBytes(Charset.forName("UTF-8")));
            deflater.finish();
            int outputSize = deflater.deflate(temp);
            deflater.reset();
            data = new byte[outputSize];
            System.arraycopy(temp, 0, data, 0, outputSize);    
        }
        return data;
    }

    //TODO Optimize!
    public static final String decompress(byte[] input) {
        String output = null;
        try {
            synchronized (tempLock) {
                inflater.setInput(input);
                int outputSize = inflater.inflate(temp);
                inflater.reset();
                output = new String(temp, 0, outputSize, Charset.forName("UTF-8"));
            }
        } catch (DataFormatException e) {
            e.printStackTrace();
        }         
        return output;
    }
    
    
    
    
    
    private static final XXHashFactory hashFactory = XXHashFactory.safeInstance();//XXHashFactory.fastestJavaInstance();

    public static final long generateHash(Path path, boolean is64Bit, byte[] buffer) throws IOException {
        //long start = System.currentTimeMillis();
        File file = path.toFile();
        long checksum = -1;
        if(is64Bit) {
            StreamingXXHash64 hasher = hashFactory.newStreamingHash64(0);
            try(FileInputStream fis = new FileInputStream(file);) {
                int read = 0;
                while((read = fis.read(buffer)) != -1) {
                    hasher.update(buffer, 0, read);
                }
            }
            checksum = hasher.getValue();
        } else {
            StreamingXXHash32 hasher = hashFactory.newStreamingHash32(0);
            try(FileInputStream fis = new FileInputStream(file);) {
                int read = 0;
                while((read = fis.read(buffer)) != -1) {
                    hasher.update(buffer, 0, read);
                }
            }
            checksum = hasher.getValue();                
        }
        
        //long duration = (System.currentTimeMillis() - start);
        //double size = (double)file.length()/(1024*1024);
        //System.out.println(path.getFileName()+" "+(size*1000/duration));
        
        return checksum;
    }
}
