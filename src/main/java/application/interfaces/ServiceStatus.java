package application.interfaces;

/**
 * Enumeration for all possible key service request status
 * @author shaun
 */
public enum ServiceStatus {
    SUCCESS,
    INVALID_USER,
    INVALID_DEVICE,
    INVALID_SIGNATURE,
    INVALID_RESPONSE,
    REQUEST_TIMED_OUT,
    CONNECTION_ERROR,
    GENERAL_FAILURE
}