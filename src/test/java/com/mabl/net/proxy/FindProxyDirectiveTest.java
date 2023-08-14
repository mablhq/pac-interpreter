package com.mabl.net.proxy;

import org.junit.Test;

import java.net.InetSocketAddress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FindProxyDirectiveTest {
    @Test
    public void direct() throws Exception {
        final FindProxyDirective directive = FindProxyDirective.parse("DIRECT");
        assertTrue(directive.isDirect());
        assertFalse(directive.isProxy());
        assertEquals(ConnectionType.DIRECT, directive.connectionType());
        assertNull(directive.proxyHostAndPort());
        assertNull(directive.proxyHost());
        assertNull(directive.proxyPort());
        assertEquals("DIRECT", directive.toString());
        assertEquals(directive, FindProxyDirective.parse("     DIRECT  "));
    }

    @Test
    public void proxy() throws Exception {
        final FindProxyDirective directive = FindProxyDirective.parse("PROXY 10.0.0.1:8080");
        assertFalse(directive.isDirect());
        assertTrue(directive.isProxy());
        assertEquals(ConnectionType.PROXY, directive.connectionType());
        assertEquals("10.0.0.1:8080", directive.proxyHostAndPort());
        assertEquals("10.0.0.1", directive.proxyHost());
        assertEquals(new Integer(8080), directive.proxyPort());
        assertEquals(InetSocketAddress.createUnresolved("10.0.0.1", 8080), directive.proxyAddress());
        assertEquals("10.0.0.1", directive.proxyAddress().getHostString());
        assertEquals(8080, directive.proxyAddress().getPort());
        assertEquals("PROXY 10.0.0.1:8080", directive.toString());
        assertEquals(directive, FindProxyDirective.parse("  PROXY   10.0.0.1:8080 "));
    }
}
