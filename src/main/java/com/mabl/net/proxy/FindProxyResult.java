package com.mabl.net.proxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Represents the result from invoking the FindProxyForURL function with a given URL.
 */
public class FindProxyResult implements Iterable<FindProxyDirective> {
    private static final String PROXY_RESULT_SEPARATOR = ";";
    private static final Random random = new Random();
    private final List<FindProxyDirective> directives;

    private FindProxyResult(final List<FindProxyDirective> directives) {
        if (directives == null) {
            throw new IllegalArgumentException("Directives must not be null");
        }
        this.directives = Collections.unmodifiableList(directives);
    }

    /**
     * Gets all proxy directives contained in this result.
     *
     * @return the list of all directives.
     */
    public List<FindProxyDirective> all() {
        return directives;
    }

    /**
     * Gets the first proxy directive contained in this result.
     *
     * @return the first directive.
     */
    public FindProxyDirective first() {
        return directives.get(0);
    }

    /**
     * Finds the first proxy directive in this result with a connection type other than {@link ConnectionType#DIRECT}.
     *
     * @return the first directive with a non-direct connection type, if any.
     */
    public Optional<FindProxyDirective> firstProxy() {
        return directives.stream()
                .filter(directive -> directive.connectionType() != ConnectionType.DIRECT)
                .findFirst();
    }

    /**
     * Gets the proxy directive with the given index.
     *
     * @param index the index of the directive to retrieve (valid values: [0, size() - 1])
     * @return the directive at the given index.
     */
    public FindProxyDirective get(final int index) {
        return directives.get(index);
    }

    @Override
    public Iterator<FindProxyDirective> iterator() {
        return directives.iterator();
    }

    /**
     * Creates a normalized copy of this {@link FindProxyResult} by removing all non-unique proxy directives
     * while maintaining the original relative ordering of the directives.
     *
     * @return a normalized copy of this result.
     */
    public FindProxyResult normalize() {
        return new FindProxyResult(new ArrayList<>(new LinkedHashSet<>(directives)));
    }

    /**
     * Gets a random proxy directive from this result.
     *
     * @return a random directive.
     */
    public FindProxyDirective random() {
        return get(random.nextInt(size()));
    }

    /**
     * Gets the number of proxy directives contained in this result.
     *
     * @return the number of directives.
     */
    public int size() {
        return directives.size();
    }

    @Override
    public String toString() {
        return directives.stream()
                .map(FindProxyDirective::toString)
                .collect(Collectors.joining(PROXY_RESULT_SEPARATOR + " "));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FindProxyResult that = (FindProxyResult) o;
        return Objects.equals(directives, that.directives);
    }

    @Override
    public int hashCode() {
        return Objects.hash(directives);
    }

    /**
     * Parses the result from the output of the FindProxyForURL function.
     *
     * @param result the output from the FindProxyForURL function.
     * @return the @{@link FindProxyResult} obtained from parsing the result.
     * @throws PacInterpreterException if the given result cannot be parsed.
     */
    public static FindProxyResult parse(final String result) throws PacInterpreterException {
        final List<FindProxyDirective> directives = new ArrayList<>();
        if (result != null) {
            for (final String directive : result.split(PROXY_RESULT_SEPARATOR)) {
                directives.add(FindProxyDirective.parse(directive));
            }
        } else {
            directives.add(FindProxyDirective.parse(null));
        }
        return new FindProxyResult(directives);
    }
}
