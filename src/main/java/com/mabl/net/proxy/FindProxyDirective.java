package com.mabl.net.proxy;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Represents a single proxy directive (e.g. DIRECT, HTTP 10.1.1.1:8080, etc.).
 */
public class FindProxyDirective {
    private static final String CONNECTION_TYPES_UNION = Arrays.stream(ConnectionType.values()).map(ConnectionType::name).collect(Collectors.joining("|"));
    private static final Pattern RESULT_PATTERN = Pattern.compile(String.format("(%s)(?:\\s+([^\\s;]+))?", CONNECTION_TYPES_UNION, Pattern.CASE_INSENSITIVE));
    private static final String HOST_PORT_DELIMITER = ":";
    private final ConnectionType connectionType;
    private final Optional<String> proxyHostAndPort;

    private FindProxyDirective(final ConnectionType connectionType) {
        this(connectionType, Optional.empty());
    }

    private FindProxyDirective(final ConnectionType connectionType, final String proxyHostAndPort) {
        this(connectionType, Optional.of(proxyHostAndPort));
    }

    private FindProxyDirective(final ConnectionType connectionType, final Optional<String> proxyHostAndPort) {
        if (connectionType == null) {
            throw new IllegalArgumentException("Connection type must not be null");
        }
        if (proxyHostAndPort == null) {
            throw new IllegalArgumentException("Proxy must not be null");
        }
        if (connectionType != ConnectionType.DIRECT && !proxyHostAndPort.isPresent()) {
            throw new IllegalArgumentException(String.format("When connection type is not %s proxy is required", ConnectionType.DIRECT));
        }
        this.connectionType = connectionType;
        this.proxyHostAndPort = proxyHostAndPort;
    }

    /**
     * Gets the connection type component of the directive, e.g. SOCKS
     *
     * @return the connection type for this directive.
     */
    public ConnectionType connectionType() {
        return connectionType;
    }

    /**
     * Tests whether this directive has connection type {@link ConnectionType#DIRECT}.
     *
     * @return true if this directive has a direct connection type; false otherwise.
     */
    public boolean isDirect() {
        return connectionType == ConnectionType.DIRECT;
    }

    /**
     * Tests whether this directive has connection type other than {@link ConnectionType#DIRECT}.
     *
     * @return true if this directive has a proxy connection type; false if the connection type is direct.
     */
    public boolean isProxy() {
        return !isDirect();
    }

    /**
     * Get the proxy address associated with this directive.
     *
     * @return the proxy address, or null if the connection type is {@link ConnectionType#DIRECT}.
     */
    public InetSocketAddress proxyAddress() {
        return proxyHostAndPort.map(hostAndPort -> {
            final String[] hostPortParts = hostAndPort.split(HOST_PORT_DELIMITER);
            final String host = hostPortParts[0];
            final int port = Integer.parseInt(hostPortParts[1]);
            return InetSocketAddress.createUnresolved(host, port);
        }).orElse(null);
    }

    /**
     * Gets the proxy host component of the directive, e.g. "192.168.1.1"
     *
     * @return the proxy host for this directive, or null if the connection type is {@link ConnectionType#DIRECT}.
     */
    public String proxyHost() {
        return Optional.ofNullable(proxyAddress()).map(InetSocketAddress::getHostString)
                .orElse(null);
    }

    /**
     * Gets the proxy port component of the directive, e.g. 8080.
     *
     * @return the proxy port for this directive, or null if the connection type is {@link ConnectionType#DIRECT}.
     */
    public Integer proxyPort() {
        return Optional.ofNullable(proxyAddress()).map(InetSocketAddress::getPort)
                .orElse(null);
    }

    /**
     * Gets the proxy and host component of the directive, e.g. "10.1.1.1:8080"
     *
     * @return the proxy:host for this directive, or null if the connection type is {@link ConnectionType#DIRECT}.
     */
    public String proxyHostAndPort() {
        return proxyHostAndPort.orElse(null);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder(connectionType.name());
        proxyHostAndPort.ifPresent(proxy -> builder.append(" ").append(proxy));
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FindProxyDirective that = (FindProxyDirective) o;
        return connectionType == that.connectionType && Objects.equals(proxyHostAndPort, that.proxyHostAndPort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectionType, proxyHostAndPort);
    }

    /**
     * Parses a single proxy directive.
     *
     * @param value the value to parse.
     * @return the parsed @{@link FindProxyDirective}.
     * @throws PacInterpreterException if the given value cannot be parsed.
     */
    public static FindProxyDirective parse(final String value) throws PacInterpreterException {
        if (value == null) {
            return new FindProxyDirective(ConnectionType.DIRECT);
        }
        final Matcher matcher = RESULT_PATTERN.matcher(value.trim());
        if (!matcher.matches()) {
            throw new PacInterpreterException(String.format("Invalid proxy find result: \"%s\"", value));
        }

        final ConnectionType connectionType;
        try {
            connectionType = ConnectionType.fromValue(matcher.group(1));
        } catch (IllegalArgumentException e) {
            // This shouldn't really happen because the regular expression is built to only accept valid connection types
            throw new PacInterpreterException(String.format("Failed to parse connection type from \"%s\"", value), e);
        }

        if (connectionType == ConnectionType.DIRECT) {
            return new FindProxyDirective(connectionType);
        }

        final String proxyHostAndPort = matcher.group(2).trim();
        return new FindProxyDirective(connectionType, proxyHostAndPort);
    }
}
