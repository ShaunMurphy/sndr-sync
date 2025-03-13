/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package application.interfaces;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Map;

/**
 * Mediator that must be implemented to perform any sync related operations
 *
 * @author shaun
 */
public interface SyncMediator {
    /**
     * Retrieves the {@link KeyPair} of the given user.
     * @param user
     * @return
     */
    byte[] getEncryptedUserKeyPair(String user);

    /**
     * Retrieves the {@link KeyPair} of the current device
     * @return the key pair or null if not found
     */
    KeyPair getDeviceKeys();

    /**
     * Retrieves a mapping of the device UUID and device {@link PublicKey} for
     * the given user.
     * @param user the user's device keys to retrieve
     * @return a mapping of the user's device keys or null
     */
    Map<String, PublicKey> getUserDevicesKeys(String user);

}