// Copyright (c) 2016 Resurface Labs LLC, All Rights Reserved

package io.resurface;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static io.resurface.JsonMessage.*;

/**
 * Logger for HTTP usage.
 */
public class HttpLogger {

    /**
     * Source name for log messages.
     */
    public static final String SOURCE = "resurfaceio-logger-java";

    /**
     * URL destination for log messages unless overridden.
     */
    public static final String URL = "https://resurfaceio.herokuapp.com/messages";

    /**
     * Initialize enabled logger using default url.
     */
    public HttpLogger() {
        this.enabled = true;
        this.url = URL;
        this.version = version_lookup();
    }

    /**
     * Initialize enabled logger using custom url.
     */
    public HttpLogger(String url) {
        this.enabled = true;
        this.url = url;
        this.version = version_lookup();
    }

    /**
     * Initialize enabled or disabled logger using custom url.
     */
    public HttpLogger(String url, boolean enabled) {
        this.enabled = enabled;
        this.url = url;
        this.version = version_lookup();
    }

    private boolean enabled;
    private boolean tracing = false;
    private final List<String> tracing_history = new ArrayList<>();
    private final String url;
    private final String version;

    /**
     * Disable this logger.
     */
    public HttpLogger disable() {
        enabled = false;
        return this;
    }

    /**
     * Enable this logger.
     */
    public HttpLogger enable() {
        enabled = true;
        return this;
    }

    /**
     * Formats JSON message for simple echo.
     */
    public StringBuilder formatEcho(StringBuilder json, long now) {
        start(json, "echo", SOURCE, version, now);
        return finish(json);
    }

    /**
     * Formats JSON message for HTTP request.
     */
    public StringBuilder formatRequest(StringBuilder json, long now, HttpServletRequest request) {
        start(json, "http_request", SOURCE, version, now).append(',');
        append(json, "url", request.getRequestURL());
        return finish(json);
    }

    /**
     * Formats JSON message for HTTP response.
     */
    public StringBuilder formatResponse(StringBuilder json, long now, HttpServletResponse response, String body) {
        start(json, "http_response", SOURCE, version, now).append(',');
        append(json, "code", response.getStatus());
        if (body != null) {
            json.append(',');
            append(json, "body", body);
        }
        return finish(json);
    }

    /**
     * Returns true if this logger is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns true if keeping a copy of all posted messages.
     */
    public boolean isTracing() {
        return tracing;
    }

    /**
     * Logs echo (in JSON format) to remote url.
     */
    public boolean logEcho() {
        if (enabled || tracing) {
            StringBuilder json = new StringBuilder(64);
            formatEcho(json, System.currentTimeMillis());
            return post(json.toString()) == 200;
        } else {
            return true;
        }
    }

    /**
     * Logs HTTP request (in JSON format) to remote url.
     */
    public boolean logRequest(HttpServletRequest request) {
        if (enabled || tracing) {
            StringBuilder json = new StringBuilder(1024);
            formatRequest(json, System.currentTimeMillis(), request);
            return post(json.toString()) == 200;
        } else {
            return true;
        }
    }

    /**
     * Logs HTTP response (in JSON format) to remote url.
     */
    public boolean logResponse(HttpServletResponse response, String body) {
        if (enabled || tracing) {
            StringBuilder json = new StringBuilder(1024);
            formatResponse(json, System.currentTimeMillis(), response, body);
            return post(json.toString()) == 200;
        } else {
            return true;
        }
    }

    /**
     * Logs message (via HTTP post) to remote url.
     */
    public int post(String json) {
        if (tracing) {
            tracing_history.add(json);
            return 200;
        } else {
            try {
                URL url = new URL(this.url);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setConnectTimeout(5000);
                con.setReadTimeout(1000);
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                try (OutputStream os = con.getOutputStream()) {
                    os.write(json.getBytes());
                    os.flush();
                }
                return con.getResponseCode();
            } catch (IOException ioe) {
                return 404;
            }
        }
    }

    /**
     * Returns unmodifiable copy of recently posted messages.
     */
    public List<String> tracingHistory() {
        return Collections.unmodifiableList(tracing_history);
    }

    /**
     * Starts keeping copy of all posted messages.
     */
    public HttpLogger tracingStart() {
        tracing = true;
        tracing_history.clear();
        return this;
    }

    /**
     * Stops tracing and clears current tracing history.
     */
    public HttpLogger tracingStop() {
        tracing = false;
        tracing_history.clear();
        return this;
    }

    /**
     * Returns url destination where messages are sent.
     */
    public String url() {
        return url;
    }

    /**
     * Returns cached version number.
     */
    public String version() {
        return version;
    }

    /**
     * Retrieves version number from runtime properties file.
     */
    public static String version_lookup() {
        try (InputStream is = HttpLogger.class.getResourceAsStream("/version.properties")) {
            Properties p = new Properties();
            p.load(is);
            return p.getProperty("version", null);
        } catch (Exception e) {
            throw new RuntimeException("Version could not be loaded: " + e.getMessage());
        }
    }

}