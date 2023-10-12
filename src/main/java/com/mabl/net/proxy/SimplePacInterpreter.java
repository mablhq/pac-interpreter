package com.mabl.net.proxy;

import com.mabl.io.IoUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * An interpreter for Proxy Auto-Configuration files/URLs.
 * <p>
 * To silence GraalVM warnings set the "polyglot.engine.WarnInterpreterOnly" system property to "false" e.g. -Dpolyglot.engine.WarnInterpreterOnly=false
 * </p>
 *
 * @see "https://developer.mozilla.org/en-US/docs/Web/HTTP/Proxy_servers_and_tunneling/Proxy_Auto-Configuration_PAC_file"
 * @see "https://www.graalvm.org/latest/reference-manual/js/FAQ/#warning-implementation-does-not-support-runtime-compilation"
 */
public class SimplePacInterpreter implements PacInterpreter {
    private static final String PAC_UTILS_PATH = "/pacUtils.js";
    private static final String PAC_LANGUAGE_ID = "js";
    private static final String PAC_FUNCTION_NAME = "FindProxyForURL";
    private static final List<Class<?>> ALLOWED_JAVA_CLASSES = Collections.unmodifiableList(Arrays.asList(
            // Allows JavaScript to invoke InetAddress methods (required for DNS/IP utility functions)
            InetAddress.class
    ));
    private static final String PAC_UTILS = readPacUtils();
    private static final Engine engine = initializeEngine();
    private final String pac;
    private final Value findProxyForUrlFunction;

    protected SimplePacInterpreter(final String pac) throws PacInterpreterException {
        this.pac = validatePac(pac);
        final Context context = initializeContext();

        // Evaluate the PAC content, and extract a reference to the PAC function:
        try {
            context.eval(PAC_LANGUAGE_ID, pac);
            final Value jsBindings = context.getBindings(PAC_LANGUAGE_ID);
            this.findProxyForUrlFunction = jsBindings.getMember(PAC_FUNCTION_NAME);
        } catch (Exception e) {
            throw new PacInterpreterException("Error evaluating PAC script", e);
        }
    }

    private static String validatePac(final String pac) {
        if (pac == null) {
            throw new IllegalArgumentException("PAC cannot be null");
        }
        if (pac.length() == 0) {
            throw new IllegalArgumentException("PAC cannot be empty");
        }
        if (!pac.contains(PAC_FUNCTION_NAME)) {
            throw new IllegalArgumentException(String.format("PAC must contain \"%s\" function", PAC_FUNCTION_NAME));
        }
        return pac;
    }

    private static Engine initializeEngine() {
        return Engine.newBuilder()
                .build();
    }

    private static Context initializeContext() {
        final Context context = Context.newBuilder(PAC_LANGUAGE_ID)
                .engine(engine)
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLoading(true)
                .allowHostClassLookup(clazz -> ALLOWED_JAVA_CLASSES.stream()
                        .map(Class::getCanonicalName)
                        .anyMatch(clazz::equals))
                .allowIO(true)
                .build();

        // Make PAC utility functions available to the context:
        context.eval(PAC_LANGUAGE_ID, PAC_UTILS);

        return context;
    }

    private static String readPacUtils() {
        try {
            return IoUtils.readClasspathFileToString(PAC_UTILS_PATH);
        } catch (IOException e) {
            // This file is included in the jar, so if we can't open/read it something is seriously wrong.
            // There is likely nothing the caller can do to handle this, so just rethrow as a runtime exception.
            throw new RuntimePacInterpreterException(String.format("Failed to read \"%s\" from classpath", PAC_UTILS_PATH), e);
        }
    }

    @Override
    public String getPac() {
        return pac;
    }

    @Override
    public FindProxyResult findProxyForUrl(final String url) throws MalformedURLException, PacInterpreterException {
        return findProxyForUrl(url, new URL(url).getHost());
    }

    @Override
    public FindProxyResult findProxyForUrl(final String url, final String host) throws PacInterpreterException {
        final String result;
        try {
            // Call the PAC function with the given URL:
            result = findProxyForUrlFunction.execute(
                            Optional.ofNullable(url).orElse(""),
                            Optional.ofNullable(host).orElse(""))
                    .asString();
        } catch (Exception e) {
            throw new PacInterpreterException(String.format("Error executing %s", PAC_FUNCTION_NAME), e);
        }
        return FindProxyResult.parse(result);
    }

    /**
     * Creates a {@link SimplePacInterpreter} using the given PAC script.
     *
     * @param pacScript the PAC script.
     * @return a {@link SimplePacInterpreter} for the given PAC script.
     * @throws PacInterpreterException if an error occurs evaluating the PAC script.
     */
    public static SimplePacInterpreter forScript(final String pacScript) throws PacInterpreterException {
        return new SimplePacInterpreter(pacScript);
    }

    /**
     * Creates a {@link SimplePacInterpreter} using the given PAC file.
     *
     * @param pacFile the PAC file.
     * @return a {@link SimplePacInterpreter} for the given PAC file.
     * @throws IOException             if an error occurs reading the PAC script from the given file.
     * @throws PacInterpreterException if an error occurs evaluating the PAC file.
     */
    public static SimplePacInterpreter forFile(final File pacFile) throws IOException, PacInterpreterException {
        return forScript(IoUtils.readFileToString(pacFile));
    }

    /**
     * Creates a {@link SimplePacInterpreter} using the given PAC URL.
     *
     * @param pacUrl the PAC URL.
     * @return a {@link SimplePacInterpreter} for the given PAC URL.
     * @throws IOException             if an error occurs reading the PAC script from the given URL.
     * @throws PacInterpreterException if an error occurs evaluating the PAC URL.
     */
    public static SimplePacInterpreter forUrl(final URL pacUrl) throws IOException, PacInterpreterException {
        return forScript(IoUtils.readUrlToString(pacUrl));
    }
}
