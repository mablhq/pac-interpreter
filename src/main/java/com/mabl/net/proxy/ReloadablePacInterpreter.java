package com.mabl.net.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * A {@link PacInterpreter} that allows the PAC script to be reloaded explicitly or automatically with a specified period.
 * <p>
 * After creating a {@link ReloadablePacInterpreter}, use the {@link #reload()} method to immediately reload the PAC.
 * Alternatively, use the {@link #start(Duration)} method to begin automatic reloads and the {@link #stop()} method to terminate the reload timer.
 *
 * @see "https://developer.mozilla.org/en-US/docs/Web/HTTP/Proxy_servers_and_tunneling/Proxy_Auto-Configuration_PAC_file"
 */
public class ReloadablePacInterpreter implements PacInterpreter {
    private static final Logger logger = LoggerFactory.getLogger(ReloadablePacInterpreter.class);
    private final Supplier<PacInterpreter> pacInterpreterSupplier;
    private volatile PacInterpreter pacInterpreter;
    private ScheduledExecutorService timer; // All access must be synchronized on AutoReloadingPacInterpreter.this

    protected ReloadablePacInterpreter(final Supplier<PacInterpreter> pacInterpreterSupplier) throws PacInterpreterException {
        if (pacInterpreterSupplier == null) {
            throw new IllegalArgumentException("PAC interpreter supplier cannot be null");
        }
        this.pacInterpreterSupplier = pacInterpreterSupplier;
        this.pacInterpreter = getPacInterpreter();
    }

    /**
     * Starts auto-updates with the given period.
     *
     * @param updatePeriod how frequently the PAC should be reloaded.
     */
    synchronized public void start(final Duration updatePeriod) {
        if (timer != null) {
            return;
        }
        timer = Executors.newSingleThreadScheduledExecutor((final Runnable runnable) -> {
            final Thread thread = new Thread(runnable, ReloadablePacInterpreter.class.getSimpleName() + " Reload Timer");
            thread.setDaemon(true);
            return thread;
        });
        timer.scheduleWithFixedDelay(this::reloadSafe, updatePeriod.toMillis(), updatePeriod.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Forces an immediate reload of the backing PAC source.
     * Calling this method has no effect on the timing of the next scheduled reload or whether the timer is started or stopped.
     *
     * @throws PacInterpreterException if an error occurs when reinitializing the underlying {@link PacInterpreter}.
     */
    public void reload() throws PacInterpreterException {
        logger.debug("Reloading PAC");
        pacInterpreter = getPacInterpreter();
        logger.debug("PAC reloaded successfully");
    }

    protected void reloadSafe() {
        try {
            reload();
        } catch (Exception e) {
            logger.error("Failed to reload PAC: " + e, e);
        }
    }

    protected PacInterpreter getPacInterpreter() throws PacInterpreterException {
        try {
            return pacInterpreterSupplier.get();
        } catch (Exception e) {
            throw new PacInterpreterException(e.getMessage(), e.getCause());
        }
    }

    /**
     * Stops auto-updates.
     */
    synchronized public void stop() {
        if (timer == null) {
            return;
        }
        timer.shutdownNow();
        timer = null;
    }

    @Override
    public String getPac() {
        return pacInterpreter.getPac();
    }

    @Override
    public FindProxyResult findProxyForUrl(final String url) throws MalformedURLException, PacInterpreterException {
        return pacInterpreter.findProxyForUrl(url);
    }

    @Override
    public FindProxyResult findProxyForUrl(final String url, final String host) throws PacInterpreterException {
        return pacInterpreter.findProxyForUrl(url, host);
    }

    /**
     * Creates an {@link ReloadablePacInterpreter} using the given PAC script supplier.
     *
     * @param pacScript supplier for the PAC script.
     * @return a {@link ReloadablePacInterpreter} for the given PAC script.
     * @throws PacInterpreterException if an error occurs evaluating the PAC script.
     */
    public static ReloadablePacInterpreter forScript(final Supplier<String> pacScript) throws PacInterpreterException {
        return new ReloadablePacInterpreter(() -> {
            try {
                return SimplePacInterpreter.forScript(pacScript.get());
            } catch (Exception e) {
                throw new RuntimePacInterpreterException(e.getMessage(), e.getCause());
            }
        });
    }

    /**
     * Creates a {@link ReloadablePacInterpreter} using the given PAC file.
     *
     * @param pacFile the PAC file.
     * @return a {@link ReloadablePacInterpreter} for the given PAC file.
     * @throws PacInterpreterException if an error occurs evaluating the PAC file.
     */
    public static ReloadablePacInterpreter forFile(final File pacFile) throws PacInterpreterException {
        return new ReloadablePacInterpreter(() -> {
            try {
                return SimplePacInterpreter.forFile(pacFile);
            } catch (Exception e) {
                throw new RuntimePacInterpreterException(e.getMessage(), e.getCause());
            }
        });
    }

    /**
     * Creates an {@link ReloadablePacInterpreter} using the given PAC URL.
     *
     * @param pacUrl the PAC URL.
     * @return a {@link ReloadablePacInterpreter} for the given PAC URL.
     * @throws PacInterpreterException if an error occurs evaluating the PAC URL.
     */
    public static ReloadablePacInterpreter forUrl(final URL pacUrl) throws PacInterpreterException {
        return new ReloadablePacInterpreter(() -> {
            try {
                return SimplePacInterpreter.forUrl(pacUrl);
            } catch (Exception e) {
                throw new RuntimePacInterpreterException(e.getMessage(), e.getCause());
            }
        });
    }
}
