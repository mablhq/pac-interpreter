package com.mabl.net.proxy;

import com.mabl.io.IoUtils;
import io.undertow.Undertow;
import io.undertow.util.Headers;
import org.junit.After;
import org.junit.Before;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

abstract public class PacInterpreterTest {
    protected static final String PAC_1 = readFromClasspath("/pac1.js");
    protected static final String PAC_2 = readFromClasspath("/pac2.js");
    protected static final String PAC_3 = readFromClasspath("/pac3.js");
    protected Undertow pacServer;
    private volatile String pacServerContent;

    @Before
    public void silenceGraalvmWarnings() {
        System.setProperty("polyglot.engine.WarnInterpreterOnly", Boolean.FALSE.toString());
    }

    @After
    public void tearDown() {
        if (pacServer != null) {
            pacServer.stop();
            pacServer = null;
            pacServerContent = null;
        }
    }

    protected static String readFromClasspath(final String path) {
        try {
            return IoUtils.readClasspathFileToString(path);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to read \"%s\" from classpath", path), e);
        }
    }

    protected static File writePacContentToFile(final String pacContent) throws IOException {
        final File pacFile = File.createTempFile("pac", ".js");
        pacFile.deleteOnExit();
        return writePacContentToFile(pacContent, pacFile);
    }

    protected static File writePacContentToFile(final String pacContent, final File pacFile) throws IOException {
        try (final BufferedWriter writer = new BufferedWriter(new FileWriter(pacFile))) {
            writer.write(pacContent);
            writer.flush();
        }
        return pacFile;
    }

    protected Undertow startPacServer(final String pacContent) {
        updatePacServerContent(pacContent);
        pacServer = Undertow.builder()
                .addHttpListener(0, "localhost")
                .setHandler(exchange -> {
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/javascript");
                    exchange.getResponseSender().send(pacServerContent);
                }).build();
        pacServer.start();
        return pacServer;
    }

    protected void updatePacServerContent(final String pacContent) {
        assertNotNull(pacContent);
        this.pacServerContent = pacContent;
    }

    protected void assertPac1Correct(final PacInterpreter interpreter) throws PacInterpreterException, MalformedURLException {
        final FindProxyResult results = interpreter.findProxyForUrl("https://example.com");
        assertEquals(2, results.size());

        final FindProxyDirective first = results.first();
        assertEquals(ConnectionType.PROXY, first.connectionType());
        assertEquals("4.5.6.7:8080", first.proxyHostAndPort());
        assertTrue(interpreter.getPac().contains(first.toString()));

        final FindProxyDirective second = results.get(1);
        assertEquals(ConnectionType.PROXY, second.connectionType());
        assertEquals("7.8.9.10:8080", second.proxyHostAndPort());
        assertTrue(interpreter.getPac().contains(second.toString()));

        final Set<FindProxyDirective> directives = new HashSet<>(results.all());
        assertTrue(directives.contains(results.random()));
    }

    protected void assertPac2Correct(final PacInterpreter interpreter) throws PacInterpreterException, MalformedURLException {
        final FindProxyResult results = interpreter.findProxyForUrl("https://example.com");
        assertEquals(1, results.size());

        final FindProxyDirective first = results.first();
        assertEquals(ConnectionType.PROXY, first.connectionType());
        assertEquals("wcg1.example.com:8080", first.proxyHostAndPort());
        assertTrue(interpreter.getPac().contains(first.toString()));
    }

    protected void assertPac3Correct(final PacInterpreter interpreter) throws PacInterpreterException, MalformedURLException {
        final FindProxyResult results = interpreter.findProxyForUrl("https://example.com");
        assertEquals(1, results.size());

        final FindProxyDirective first = results.first();
        assertEquals(ConnectionType.DIRECT, first.connectionType());
        assertNull(first.proxyHostAndPort());
    }

}
