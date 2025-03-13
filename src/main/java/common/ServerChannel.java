package common;

import com.sndr.logger.SndrLogger;
import util.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ServerChannel implements AutoCloseable {
    private static final Logger logger = SndrLogger.getLogger();

    private ServerSocketChannel serverChannel;
    private InetSocketAddress serverSocket;
    private ClientChannel clientChannel;
    //private boolean isClosed = false;
    private boolean connectionAccepted = false;

    public ServerChannel(final InetAddress localAddress) throws IOException {
        this(localAddress, 0);
    }

    public ServerChannel(final InetAddress localAddress, final int port) throws IOException {
        //Port is 0 to allow the OS to assign one.
        final InetSocketAddress localSocketAddress = new InetSocketAddress(localAddress, port);
        this.serverChannel = ServerSocketChannel.open();
        serverChannel.socket().bind(localSocketAddress, Config.MAX_SOCKET_CONNECTIONS);
        this.serverSocket = (InetSocketAddress) serverChannel.socket().getLocalSocketAddress();
    }

    @Override
    public void close() {
        if(this.serverChannel != null) {
            try {
                this.serverChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(this.clientChannel != null) {
            this.clientChannel.close();
        }
    }

    public final InetSocketAddress getServerSocketAddress() {
        return this.serverSocket;
    }
    
   /** This will block the thread for {@link Config#SOCKET_TIMEOUT} seconds or until the client connects.
    * @return true if successful.*/
   @SuppressWarnings("resource")
   public final boolean acceptServerChannelConnection() {
       try {
           Selector selector = Selector.open();
           this.serverChannel.configureBlocking(false);
           this.serverChannel.register(selector, SelectionKey.OP_ACCEPT, this);
           logger.log(Level.INFO, "Waiting for the client to connect.");
           this.serverChannel.socket().setSoTimeout(Config.SERVER_SOCKET_TIMEOUT * 1000);

           //This is a fix in case the setSoTimeout() does not fire after the timeout should have occurred.
           if(selector.select(Config.SERVER_SOCKET_TIMEOUT * 1000) == 0) {
               selector.close();
               throw new SocketTimeoutException();
           }
           selector.close();

           //Accept incoming connections.
           this.serverChannel.configureBlocking(true);
           SocketChannel channel = this.serverChannel.socket().accept().getChannel();
           this.clientChannel = new ClientChannel(channel);
           //this.clientChannel = this.serverChannel.accept();//Doesn't work?
           //this.localSocket = ((InetSocketAddress)this.serverChannel.getLocalAddress());
           connectionAccepted = channel != null;
           return true;
       } catch (SocketException e) {
           logger.log(Level.SEVERE, "Error creating or accessing the socket channel.", e);
       } catch (SocketTimeoutException e) {
           logger.log(Level.FINE, "Waited too long for the remote socket to connect.", e);
       } catch (IOException e) {
           logger.log(Level.FINE, "", e);
       }
       return false;
   }

   public final ServerSocketChannel getServerChannel() {
       return this.serverChannel;
   }
   
   public final ClientChannel getClientChannel() {
       return this.clientChannel;
   }
   
   public final InputStream getInputStream() throws IOException {
       if(this.clientChannel != null) {
           return this.clientChannel.getInputStream();
       }
       return null;
   }
   
   public final OutputStream getOutputStream() throws IOException {
       if(this.clientChannel != null) {
           return this.clientChannel.getOutputStream();
       }
       return null;
   }
   
   public final boolean wasConnectionAccepted() {
       return this.connectionAccepted;
   }
}