package com.mabl.net.proxy;

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
     * Gets the proxy and host component of the directive, e.g. "10.1.1.1:8080"
     *
     * @return the proxy:host for this directive.
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
