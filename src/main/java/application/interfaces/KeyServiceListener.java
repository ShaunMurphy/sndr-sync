package application.interfaces;

public interface KeyServiceListener {
    /**
     * Returns the user's private key.
     * @param userPrivateKey 
     */
    public void succeeded(byte[] userPrivateKey);
    
    /**
     * Failed to get the user's private key.
     * @param status - Why it failed.
     */
    public void failed(ServiceStatus status);
}