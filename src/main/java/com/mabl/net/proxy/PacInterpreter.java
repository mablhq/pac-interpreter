package com.mabl.net.proxy;

import java.net.MalformedURLException;

public interface PacInterpreter {
    /**
     * Gets the PAC that is in use by this @{@link SimplePacInterpreter}.
     *
     * @return the PAC contents.
     */
    String getPac();

    /**
     * Evaluates the PAC script for the given URL.
     * Automatically parses the host from the URL before passing it to the PAC script.
     *
     * @param url the URL to evaluate.
     * @return the result of executing the PAC script with the given URL.
     * @throws MalformedURLException   if the URL cannot be parsed.
     * @throws PacInterpreterException if an error occurs evaluating the PAC script or parsing the results.
     */
    FindProxyResult findProxyForUrl(final String url) throws MalformedURLException, PacInterpreterException;

    /**
     * Evaluates the PAC script for the given URL and host.
     *
     * @param url  the URL to evaluate.
     * @param host the host component of the URL (the URL substring between :// and the first : or /).
     * @return the result of executing the PAC script with the given URL and host.
     * @throws PacInterpreterException if an error occurs evaluating the PAC script or parsing the results.
     */
    FindProxyResult findProxyForUrl(final String url, final String host) throws PacInterpreterException;
}
