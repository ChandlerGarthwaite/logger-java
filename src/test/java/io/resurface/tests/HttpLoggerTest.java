// © 2016-2017 Resurface Labs LLC

package io.resurface.tests;

import io.resurface.HttpLogger;
import io.resurface.UsageLoggers;
import org.junit.Test;

import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static io.resurface.tests.Helper.MOCK_URLS_DENIED;

/**
 * Tests against usage logger for HTTP/HTTPS protocol.
 */
public class HttpLoggerTest {

    @Test
    public void createsInstanceTest() {
        HttpLogger logger = new HttpLogger();
        expect(logger).toBeNotNull();
        expect(logger.getAgent()).toEqual(HttpLogger.AGENT);
        expect(logger.isEnabled()).toBeFalse();
    }

    @Test
    public void createsMultipleInstancesTest() {
        String url1 = "http://resurface.io";
        String url2 = "http://whatever.com";
        HttpLogger logger1 = new HttpLogger(url1);
        HttpLogger logger2 = new HttpLogger(url2);
        HttpLogger logger3 = new HttpLogger(Helper.DEMO_URL);

        expect(logger1.getAgent()).toEqual(HttpLogger.AGENT);
        expect(logger1.isEnabled()).toBeTrue();
        expect(logger1.getUrl()).toEqual(url1);
        expect(logger2.getAgent()).toEqual(HttpLogger.AGENT);
        expect(logger2.isEnabled()).toBeTrue();
        expect(logger2.getUrl()).toEqual(url2);
        expect(logger3.getAgent()).toEqual(HttpLogger.AGENT);
        expect(logger3.isEnabled()).toBeTrue();
        expect(logger3.getUrl()).toEqual(Helper.DEMO_URL);

        UsageLoggers.disable();
        expect(UsageLoggers.isEnabled()).toBeFalse();
        expect(logger1.isEnabled()).toBeFalse();
        expect(logger2.isEnabled()).toBeFalse();
        expect(logger3.isEnabled()).toBeFalse();
        UsageLoggers.enable();
        expect(UsageLoggers.isEnabled()).toBeTrue();
        expect(logger1.isEnabled()).toBeTrue();
        expect(logger2.isEnabled()).toBeTrue();
        expect(logger3.isEnabled()).toBeTrue();
    }

    @Test
    public void detectsStringContentTypesTest() {
        HttpLogger logger = new HttpLogger();

        expect(logger.isStringContentType("application/json")).toBeTrue();
        expect(HttpLogger.isStringContentType("application/soap")).toBeTrue();
        expect(HttpLogger.isStringContentType("application/xml")).toBeTrue();
        expect(HttpLogger.isStringContentType("application/x-www-form-urlencoded")).toBeTrue();
        expect(HttpLogger.isStringContentType("text/html")).toBeTrue();
        expect(HttpLogger.isStringContentType("text/html; charset=utf-8")).toBeTrue();
        expect(HttpLogger.isStringContentType("text/plain")).toBeTrue();
        expect(HttpLogger.isStringContentType("text/plain123")).toBeTrue();
        expect(HttpLogger.isStringContentType("text/xml")).toBeTrue();
        expect(HttpLogger.isStringContentType("Text/XML")).toBeTrue();

        expect(logger.isStringContentType("application/")).toBeFalse();
        expect(HttpLogger.isStringContentType("json")).toBeFalse();
        expect(HttpLogger.isStringContentType("html")).toBeFalse();
        expect(HttpLogger.isStringContentType("xml")).toBeFalse();
    }

    @Test
    public void hasValidAgentTest() {
        String agent = HttpLogger.AGENT;
        expect(agent.length()).toBeGreaterThan(0);
        expect(agent).toEndWith(".java");
        expect(agent.contains("\\")).toBeFalse();
        expect(agent.contains("\"")).toBeFalse();
        expect(agent.contains("'")).toBeFalse();
        expect(new HttpLogger().getAgent()).toEqual(agent);
    }

    @Test
    public void skipsLoggingWhenDisabledTest() {
        for (String url : MOCK_URLS_DENIED) {
            HttpLogger logger = new HttpLogger(url).disable();
            expect(logger.isEnabled()).toBeFalse();
            expect(logger.log(null, null, null, null)).toBeTrue();  // would fail if enabled
        }
    }

}
