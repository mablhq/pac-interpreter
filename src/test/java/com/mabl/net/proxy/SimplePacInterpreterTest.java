package com.mabl.net.proxy;

import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SimplePacInterpreterTest extends PacInterpreterTest {

    @Test
    public void forScript() throws Exception {
        final SimplePacInterpreter interpreter = SimplePacInterpreter.forScript(PAC_1);
        assertPac1Correct(interpreter);
    }

    @Test
    public void forFile() throws Exception {
        final SimplePacInterpreter interpreter = SimplePacInterpreter.forFile(writePacContentToFile(PAC_2));
        assertPac2Correct(interpreter);
    }

    @Test
    public void forUrl() throws Exception {
        final InetSocketAddress serverAddress = (InetSocketAddress) startPacServer(PAC_3).getListenerInfo().get(0).getAddress();
        final SimplePacInterpreter interpreter = SimplePacInterpreter.forUrl(new URL(String.format("http://%s:%d/pac.js", serverAddress.getAddress().getHostAddress(), serverAddress.getPort())));
        assertPac3Correct(interpreter);
    }

    @Test
    public void nullMapsToDirect() throws Exception {
        final String pacFileContent = "function FindProxyForURL(url, host) { return null; }";
        final SimplePacInterpreter interpreter = SimplePacInterpreter.forScript(pacFileContent);
        final FindProxyResult results = interpreter.findProxyForUrl("https://example.com");
        assertEquals(1, results.size());

        // null should map to DIRECT
        final FindProxyDirective first = results.first();
        assertEquals(ConnectionType.DIRECT, first.connectionType());
        assertNull(first.proxyHostAndPort());
    }

    @Test
    public void undefinedMapsToDirect() throws Exception {
        final String pacFileContent = "function FindProxyForURL(url, host) { return undefined; }";
        final SimplePacInterpreter interpreter = SimplePacInterpreter.forScript(pacFileContent);
        final FindProxyResult results = interpreter.findProxyForUrl("https://example.com");
        assertEquals(1, results.size());

        // undefined should map to DIRECT
        final FindProxyDirective first = results.first();
        assertEquals(ConnectionType.DIRECT, first.connectionType());
        assertNull(first.proxyHostAndPort());
    }

}
