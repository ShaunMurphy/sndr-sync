package icelink;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import util.Config.ConnectionType;
import common.connection.IceLinkConnection;

public final class IceLink_Main {
    private final List<AbstractMap.SimpleImmutableEntry<IceLinkConnection, CountDownLatch>> pending = new ArrayList<>();
    private server.connection.ConnectionManager serverConnectionManager;
    public IceLink_Main() {
        
    }
    
    public final void setServerConnectionManager(server.connection.ConnectionManager manager) {
        this.serverConnectionManager = manager;
    }

    public void lockThreadUntilNotify(IceLinkConnection connection) {
        final CountDownLatch latch = new CountDownLatch(1);
        pending.add(new AbstractMap.SimpleImmutableEntry<IceLinkConnection, CountDownLatch>(connection, latch));
        try {
            System.out.println(Thread.currentThread().getName()+" is blocked!");
            boolean countedDown = latch.await(1, TimeUnit.MINUTES);
            if(!countedDown) {
                System.out.println("Connection latch timed-out!!!");
            } else {
                System.out.println(Thread.currentThread().getName()+" is unblocked!");                
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    public final void notifyAnswerAvailable(String connectionUuid) {
        CountDownLatch latch = null;
        Iterator<AbstractMap.SimpleImmutableEntry<IceLinkConnection, CountDownLatch>> iterator = pending.iterator();
        while(iterator.hasNext()) {
            AbstractMap.SimpleImmutableEntry<IceLinkConnection, CountDownLatch>item = iterator.next();
            IceLinkConnection c = item.getKey();
            String test = c.getRemoteConnection().getConnectionId();
            //System.out.println(test+"=?="+connectionUuid);
            if(test.equals(connectionUuid)) {
                latch = item.getValue();
                iterator.remove();
                //System.out.println("icelink connection = "+c);
                break;
            }
        }

        //Unlock the thread for the connection.
        latch.countDown();
    }

    public void notifyOfferAvailable(String remoteDeviceUuid, String connectionUuid) {
        if(this.serverConnectionManager != null) {
            //String remoteDeviceUuidAndConnectionUuid = remoteDeviceUuid+","+connectionUuid;
            this.serverConnectionManager.createConnection(ConnectionType.ICELINK, remoteDeviceUuid, connectionUuid);
        }
    }
}
