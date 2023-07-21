package com.mabl.net.proxy;

import java.util.Arrays;

/**
 * Represents the connection type returned by the PAC function
 *
 * @see "https://developer.mozilla.org/en-US/docs/Web/HTTP/Proxy_servers_and_tunneling/Proxy_Auto-Configuration_PAC_file#return_value_format"
 */
public enum ConnectionType {
    DIRECT,
    HTTP,
    HTTPS,
    PROXY,
    SOCKS,
    SOCKS4,
    SOCKS5;

    public static ConnectionType fromValue(final String value) {
        return Arrays.stream(values()).filter(ct -> ct.name().equalsIgnoreCase(value)).findAny().orElseThrow(() ->
                new IllegalArgumentException(String.format("\"%s\" is not a valid %s", value, ConnectionType.class.getSimpleName()))
        );
    }
}
