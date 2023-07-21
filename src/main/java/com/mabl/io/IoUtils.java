package com.mabl.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class IoUtils {
    public static String readClasspathFileToString(final String path) throws IOException {
        try (final InputStream resourceIn = IoUtils.class.getResourceAsStream(path)) {
            return readInputStreamToString(resourceIn);
        }
    }

    public static String readUrlToString(final URL pacUrl) throws IOException {
        try (final InputStream urlIn = pacUrl.openConnection().getInputStream()) {
            return readInputStreamToString(urlIn);
        }
    }

    public static String readFileToString(final File file) throws IOException {
        try (final InputStream fileIn = new FileInputStream(file)) {
            return readInputStreamToString(fileIn);
        }
    }

    public static String readInputStreamToString(final InputStream in) throws IOException {
        final StringBuffer buffer = new StringBuffer();
        try (final BufferedReader utilsIn = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = utilsIn.readLine()) != null) {
                buffer.append(line).append("\n");
            }
        }
        return buffer.toString();
    }
}
