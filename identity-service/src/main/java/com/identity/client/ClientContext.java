package com.identity.client;

/**
    Who is calling, as established by the server: the IP comes from the connection
    (after the trusted-proxy valve), the device ID from a cookie we signed.
    Nothing here is read from the body.
 */
public record ClientContext(String ip, String deviceId) {
}
