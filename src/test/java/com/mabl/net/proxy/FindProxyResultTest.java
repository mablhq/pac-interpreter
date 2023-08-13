package com.mabl.net.proxy;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FindProxyResultTest {
    @Test
    public void all() throws Exception {
        final FindProxyResult result = FindProxyResult.parse("PROXY 10.0.0.1:8080; SOCKS 10.0.0.1:1080; DIRECT");
        final List<FindProxyDirective> directives = result.all();

        final FindProxyDirective directive1 = directives.get(0);
        assertEquals(ConnectionType.PROXY, directive1.connectionType());
        assertEquals("10.0.0.1:8080", directive1.proxyHostAndPort());

        final FindProxyDirective directive2 = directives.get(1);
        assertEquals(ConnectionType.SOCKS, directive2.connectionType());
        assertEquals("10.0.0.1:1080", directive2.proxyHostAndPort());

        final FindProxyDirective directive3 = directives.get(2);
        assertEquals(ConnectionType.DIRECT, directive3.connectionType());
        assertNull(directive3.proxyHostAndPort());
    }

    @Test
    public void first() throws Exception {
        final FindProxyResult result = FindProxyResult.parse("PROXY 10.0.0.1:8080; SOCKS 10.0.0.1:1080; DIRECT");
        final FindProxyDirective first = result.first();
        assertEquals(ConnectionType.PROXY, first.connectionType());
        assertEquals("10.0.0.1:8080", first.proxyHostAndPort());
    }

    @Test
    public void firstProxyWithProxyPresent() throws Exception {
        final FindProxyResult result = FindProxyResult.parse("DIRECT; PROXY 10.0.0.1:8080; SOCKS 10.0.0.1:1080");
        final Optional<FindProxyDirective> maybeFirstProxy = result.firstProxy();
        assertTrue(maybeFirstProxy.isPresent());
        final FindProxyDirective firstProxy = maybeFirstProxy.get();
        assertEquals(ConnectionType.PROXY, firstProxy.connectionType());
        assertEquals("10.0.0.1:8080", firstProxy.proxyHostAndPort());
    }

    @Test
    public void firstProxyWithNoProxyPresent() throws Exception {
        final FindProxyResult result = FindProxyResult.parse("DIRECT");
        final Optional<FindProxyDirective> maybeFirstProxy = result.firstProxy();
        assertFalse(maybeFirstProxy.isPresent());
    }

    @Test
    public void get() throws Exception {
        final FindProxyResult result = FindProxyResult.parse("PROXY 10.0.0.1:8080; SOCKS 10.0.0.1:1080; DIRECT");

        final FindProxyDirective directive1 = result.get(0);
        assertEquals(ConnectionType.PROXY, directive1.connectionType());
        assertEquals("10.0.0.1:8080", directive1.proxyHostAndPort());

        final FindProxyDirective directive2 = result.get(1);
        assertEquals(ConnectionType.SOCKS, directive2.connectionType());
        assertEquals("10.0.0.1:1080", directive2.proxyHostAndPort());

        final FindProxyDirective directive3 = result.get(2);
        assertEquals(ConnectionType.DIRECT, directive3.connectionType());
        assertNull(directive3.proxyHostAndPort());
    }

    @Test(expected = PacInterpreterException.class)
    public void invalid() throws Exception {
        FindProxyResult.parse("FOO");
        fail("Parsing should have failed");
    }

    @Test
    public void iterator() throws Exception {
        final FindProxyResult result = FindProxyResult.parse("PROXY 10.0.0.1:8080; SOCKS 10.0.0.1:1080; DIRECT");
        final List<FindProxyDirective> directives = new ArrayList<>(result.size());
        result.iterator().forEachRemaining(directives::add);

        final FindProxyDirective directive1 = directives.get(0);
        assertEquals(ConnectionType.PROXY, directive1.connectionType());
        assertEquals("10.0.0.1:8080", directive1.proxyHostAndPort());

        final FindProxyDirective directive2 = directives.get(1);
        assertEquals(ConnectionType.SOCKS, directive2.connectionType());
        assertEquals("10.0.0.1:1080", directive2.proxyHostAndPort());

        final FindProxyDirective directive3 = directives.get(2);
        assertEquals(ConnectionType.DIRECT, directive3.connectionType());
        assertNull(directive3.proxyHostAndPort());
    }

    @Test
    public void normalize() throws Exception {
        final FindProxyResult result = FindProxyResult.parse("PROXY 10.0.0.1:8080; PROXY 10.0.0.1:8080; DIRECT; SOCKS 10.0.0.1:1080; DIRECT; DIRECT");
        assertEquals(6, result.size());

        final FindProxyResult normalized = result.normalize();
        assertEquals(3, normalized.size());

        final FindProxyDirective directive1 = normalized.get(0);
        assertEquals(ConnectionType.PROXY, directive1.connectionType());
        assertEquals("10.0.0.1:8080", directive1.proxyHostAndPort());

        final FindProxyDirective directive2 = normalized.get(1);
        assertEquals(ConnectionType.DIRECT, directive2.connectionType());
        assertNull(directive2.proxyHostAndPort());

        final FindProxyDirective directive3 = normalized.get(2);
        assertEquals(ConnectionType.SOCKS, directive3.connectionType());
        assertEquals("10.0.0.1:1080", directive3.proxyHostAndPort());
    }

    @Test
    public void random() throws Exception {
        final FindProxyResult result = FindProxyResult.parse("PROXY 10.0.0.1:8080; SOCKS 10.0.0.1:1080; DIRECT");
        final Set<FindProxyDirective> directives = new HashSet<>(result.all());
        for (int ii = 0; ii < 10; ii++) {
            assertTrue(directives.contains(result.random()));
        }
    }

    @Test
    public void resultToString() throws Exception {
        final FindProxyResult result = FindProxyResult.parse("PROXY    10.0.0.1:8080;   SOCKS  10.0.0.1:1080;  DIRECT ");
        assertEquals("PROXY 10.0.0.1:8080; SOCKS 10.0.0.1:1080; DIRECT", result.toString());
    }

    @Test
    public void size() throws Exception {
        final FindProxyResult result = FindProxyResult.parse("PROXY 10.0.0.1:8080; SOCKS 10.0.0.1:1080; DIRECT");
        assertEquals(3, result.size());
    }
}
