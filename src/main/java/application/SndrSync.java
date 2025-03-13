package application;

import java.io.File;

import application.interfaces.ApiHelper;
import application.interfaces.Crypto;
import application.interfaces.SyncMediator;
import client.SndrBlockClient;
import common.Serializer;
import common.manager.CoreManager;
import icelink.IceLink_Main;
import server.SndrBlockServer;
import ssdp.message.SsdpConstants;
import util.Config;

public class SndrSync {

    public interface LifecycleListener {

        void onStateChanged(State state);
    }

    private final SndrBlockServer server;
    private final SndrBlockClient client;
    private final IceLink_Main icelink;
    private final SSDP_Main ssdpMain;
    private final SndrBlockApi blockApi;
    //private String user;
    private LifecycleListener listener;
    private State state = State.INITIALIZED;

    public enum State {
        INITIALIZED, STARTING, STARTED, STOPPING, STOPPED, SHUTTING_DOWN, SHUTDOWN;
    }

    public SndrSync(final Crypto crypto, ApiHelper apiHelper, final SyncMediator syncMediator, String thisDeviceUUID, File rootDirectory, SsdpConstants.DEVICE_TYPES deviceType) {
        setRootDirectory(rootDirectory);
        this.icelink = new IceLink_Main();
        this.server = new SndrBlockServer(icelink, crypto, apiHelper);
        this.client = new SndrBlockClient(icelink, crypto, apiHelper);
        this.ssdpMain = new SSDP_Main(deviceType, thisDeviceUUID, syncMediator);
        this.blockApi = new SndrBlockApi(client, apiHelper);

        Serializer.INSTANCE.initialize(crypto);
    }

    public final void setUser(String user) {
        //this.user = user;
    }

    public void setListener(LifecycleListener listener) {
        this.listener = listener;
    }

    /**
     * Once this is called, all file system related calls after calling this
     * method will use the new root directory.
     *
     * @param rootDirectory
     */
    public final void setRootDirectory(File rootDirectory) {
        if (rootDirectory == null) {
            throw new IllegalArgumentException("The root directory cannot be null.");
        }
        Config.setRootDirectory(rootDirectory);
    }

    public final void start() {
        checkShuttingDown();

        if (this.state.equals(State.INITIALIZED) || this.state.equals(State.STOPPED)) {
            setState(State.STARTING);
            this.ssdpMain.start();
            this.ssdpMain.setCreateConnectionHandler(this.server.getCreateConnectionHandler());
            setState(State.STARTED);
        }
    }

    public final void stop() {
        checkShuttingDown();

        if (this.state.equals(State.STARTED)) {
            setState(State.STOPPING);
            this.ssdpMain.stop();
            setState(State.STOPPED);
        }
    }

    public final void shutdown() {
        if (!this.state.equals(State.SHUTDOWN) && !this.state.equals(State.SHUTTING_DOWN)) {
            setState(State.SHUTTING_DOWN);
            this.ssdpMain.stop();
            this.server.shutdownServer();
            CoreManager.get().shutdown();
            setState(State.SHUTDOWN);
        }
    }

    public final SndrBlockApi getApi() {
        if (!this.state.equals(State.STARTED)) {
            throw new IllegalStateException("This must be started before calling this method.");
        }
        return this.blockApi;
    }

    public final void notifyOfferAvailable(String remoteDeviceUuid, String connectionUuid) {
        this.icelink.notifyOfferAvailable(remoteDeviceUuid, connectionUuid);
    }

    public final void notifyAnswerAvailable(String connectionUuid) {
        this.icelink.notifyAnswerAvailable(connectionUuid);
    }

    private void checkShuttingDown() {
        if (this.state.equals(State.SHUTDOWN) || this.state.equals(State.SHUTTING_DOWN)) {
            throw new IllegalStateException("Cannot perform action after shutdown.");
        }
    }

    private void setState(State state) {
        this.state = state;
        if (null != listener) {
            listener.onStateChanged(state);
        }
    }
}