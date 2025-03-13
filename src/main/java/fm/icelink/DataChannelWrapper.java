package fm.icelink;

import fm.icelink.DataBuffer;
import fm.icelink.DataChannel;

public final class DataChannelWrapper extends DataChannel { 
    private static final IAction1<Exception> defaultFailAction = new IAction1<Exception>() {
        @Override
        public void invoke(Exception e) {
            e.printStackTrace();
        }
    };
    public DataChannelWrapper(String label) {
        super(label);
    }

    @Override
    public void sendDataBytes(DataBuffer dataBytes) {
        //Fixes the null pointer exception when this fails.
        super.getInnerDataChannel().sendBytes(dataBytes, null, defaultFailAction);
    }

    @Override
    public void sendDataString(String dataString) {
        super.getInnerDataChannel().sendString(dataString, null, defaultFailAction);
    }
}