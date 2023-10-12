package com.mabl.net.proxy;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

public class ReloadablePacInterpreterTest extends PacInterpreterTest {
    private ReloadablePacInterpreter pacInterpreter;

    @After
    public void stopReload() {
        if (pacInterpreter != null) {
            pacInterpreter.stop();
            pacInterpreter = null;
        }
    }

    @Test
    public void forScript() throws Exception {
        final AtomicReference<String> script = new AtomicReference<>(PAC_1);

        pacInterpreter = ReloadablePacInterpreter.forScript(script::get);
        assertPac1Correct(pacInterpreter);

        script.set(PAC_2);
        pacInterpreter.reload();
        assertPac2Correct(pacInterpreter);
    }

    @Test
    public void forFile() throws Exception {
        final File pacFile = writePacContentToFile(PAC_1);

        pacInterpreter = ReloadablePacInterpreter.forFile(pacFile);
        assertPac1Correct(pacInterpreter);

        writePacContentToFile(PAC_2, pacFile);
        pacInterpreter.reload();
        assertPac2Correct(pacInterpreter);
    }

    @Test
    public void forServer() throws Exception {
        final InetSocketAddress serverAddress = (InetSocketAddress) startPacServer(PAC_2).getListenerInfo().get(0).getAddress();

        pacInterpreter = ReloadablePacInterpreter.forUrl(new URL(String.format("http://%s:%d/pac.js", serverAddress.getAddress().getHostAddress(), serverAddress.getPort())));
        assertPac2Correct(pacInterpreter);

        updatePacServerContent(PAC_3);
        pacInterpreter.reload();
        assertPac3Correct(pacInterpreter);
    }

    @Test
    public void timer() throws Exception {
        final AtomicReference<String> script = new AtomicReference<>(PAC_1);

        pacInterpreter = ReloadablePacInterpreter.forScript(script::get);
        assertPac1Correct(pacInterpreter);

        final Duration reloadPeriod = Duration.ofSeconds(1);
        script.set(PAC_2);
        pacInterpreter.start(reloadPeriod);
        assertPac1Correct(pacInterpreter);

        Thread.sleep(reloadPeriod.toMillis() * 2);
        assertPac2Correct(pacInterpreter);
    }
}
