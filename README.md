# resurfaceio-logger-java
&copy; 2016 Resurface Labs LLC, All Rights Reserved

This library makes it easy to log actual usage of Java web/json apps.

## Contents

<ul>
<li><a href="#dependencies">Dependencies</a></li>
<li><a href="#installing_with_maven">Installing With Maven</a></li>
<li><a href="#logging_from_spark_framework">Logging From Spark Framework</a></li>
<li><a href="#logging_from_servlet_filter">Logging From Servlet Filter</a></li>
<li><a href="#logging_to_different_urls">Logging To Different URLs</a></li>
<li><a href="#advanced_topics">Advanced Topics</a><ul>
<li><a href="#setting_default_url">Setting Default URL</a></li>
<li><a href="#disabling_all_logging">Disabling All Logging</a></li>
<li><a href="#using_api_directly">Using API Directly</a></li>
</ul></li>
</ul>

<a name="dependencies"/>

## Dependencies

Requires Java 8. No other dependencies to conflict with your app.

<a name="installing_with_maven"/>

## Installing with Maven

Use these sections in your pom.xml:

    <dependencies>
        <dependency>
            <groupId>io.resurface</groupId>
            <artifactId>resurfaceio-logger</artifactId>
            <version>1.5.0-SNAPSHOT</version>
        </dependency>
    </dependencies>

    <repositories>
        <repository>
            <id>resurfaceio-mavenrepo</id>
            <url>https://github.com/resurfaceio/resurfaceio-mavenrepo/raw/master</url>
            <releases><enabled>false</enabled></releases>
            <snapshots><enabled>true</enabled></snapshots>
        </repository>
    </repositories>

<a name="logging_from_spark_framework"/>

## Logging From Spark Framework

Spark Framework is a popular microservice framework, and is featured by Heroku's
[Getting Started with Java](https://devcenter.heroku.com/articles/getting-started-with-java) tutorial.

After <a href="#installing_with_maven">installing the library</a>, use a logger within Spark handlers as shown below.

    import io.resurface.HttpLogger;

    HttpLogger logger = new HttpLogger("DEMO");

    get("/hello", (request, response) -> {
        String response_body = "Hello World";
        logger.log(request.raw(), null, response.raw(), response_body);
        return response_body;
    });

    post("/hello_post", (request, response) -> {
        response.status(401);
        logger.log(request.raw(), request.body(), response.raw(), null);
        return "";
    });

Usage data will now be logged here:
[https://demo-resurfaceio.herokuapp.com/messages](https://demo-resurfaceio.herokuapp.com/messages)

*Resurface Labs provides this free demo environment just to make our stuff easy to try. Data sent to this demo
environment is held in volatile memory for less than 24 hours, and is never shared with third parties.
(<a href="">Privacy and Terms of Service</a>)*

NOTE: When integrating with Spark, logging from before/after filters is discouraged.

<a name="logging_from_servlet_filter"/>

## Logging From Servlet Filter

This works for Tomcat, Jetty and other application servers that support standard servlet filters. Unlike the example for
Spark above, this requires no changes to your app, and logs response headers that are not seen by the app.

This logger performs some basic filtering: it ignores redirects (304 response codes), and only logs responses for content
types matching a predefined list (including 'text/html' and 'application/json').

Start by <a href="#installing_with_maven">installing the library</a> or by simply copying resurfaceio-logger-X.X.X.jar
into the appropriate /lib directory. (no other dependencies required)

Now configure the filter in web.xml as shown below. You can optionally use a specific url-pattern to limit logging
to specific areas of your application.

    <filter>
        <filter-name>HttpLoggerForServlets</filter-name>
        <filter-class>io.resurface.HttpLoggerForServlets</filter-class>
        <init-param>
            <param-name>url</param-name>
            <param-value>DEMO</param-value>
        </init-param>
    </filter>
    <filter-mapping>
        <filter-name>HttpLoggerForServlets</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>
    
Usage data will now be logged here:
[https://demo-resurfaceio.herokuapp.com/messages](https://demo-resurfaceio.herokuapp.com/messages)

*Resurface Labs provides this free demo environment just to make our stuff easy to try. Data sent to this demo
environment is held in volatile memory for less than 24 hours, and is never shared with third parties.
(<a href="">Privacy and Terms of Service</a>)*

<a name="logging_to_different_urls"/>

## Logging To Different URLs

Our loggers don't lock you into using any particular backend service. Loggers can send data to any URL that accepts JSON
messages as a standard HTTPS POST.

    // for basic logger
    HttpLogger logger = new HttpLogger("https://my-https-url");

    // for servlet filter
    <filter>
        <filter-name>HttpLoggerForServlets</filter-name>
        <filter-class>io.resurface.HttpLoggerForServlets</filter-class>
        <init-param>
            <param-name>url</param-name>
            <param-value>https://my-other-url</param-value>
        </init-param>
    </filter>

As implied in the fake example above, a single app can have several loggers that send usage data to different URLs at once.

<a name="advanced_topics"/>

## Advanced Topics

<a name="setting_default_url"/>

### Setting Default URL

Set the USAGE_LOGGERS_URL variable to provide a default value whenever the URL is not specified. This is most useful when you
intend to have multiple loggers using a single backend service.

    // using Heroku cli
    heroku config:set USAGE_LOGGERS_URL=https://my-https-url

    // when starting Java container
    java -DUSAGE_LOGGERS_URL="https://my-https-url" your_application

Loggers look for this environment variable when no other options are set, as in these examples.

    // for basic logger
    HttpLogger logger = new HttpLogger();

    // for servlet filter
    <filter>
        <filter-name>HttpLoggerForServlets</filter-name>
        <filter-class>io.resurface.HttpLoggerForServlets</filter-class>
    </filter>

<a name="disabling_all_logging"/>

### Disabling All Logging

It's important to have a "kill switch" to universally disable all logging. For example, loggers might be disabled when
running automated tests. All loggers can also be disabled at runtime, either by setting an environment variable or
programmatically.

    # for Heroku app
    heroku config:set USAGE_LOGGERS_DISABLE=true

    # when starting Java container
    java -DUSAGE_LOGGERS_DISABLE="true"

    # at runtime
    UsageLoggers.disable();

<a name="using_api_directly"/>

### Using API Directly

Loggers can be directly integrated into your application if other options don't fit. This requires the most effort, but
yields complete control over what details are logged, and where logged data is sent.

    import io.resurface.*;

    // manage all loggers (even those not created yet)
    UsageLoggers.disable();                                      // disable all loggers
    UsageLoggers.enable();                                       // enable all loggers

    // create and configure logger
    HttpLogger logger;
    logger = new HttpLogger(queue);                              // log to appendable queue
    logger = new HttpLogger(queue, false);                       // (initially disabled)
    logger = new HttpLogger(my_https_url);                       // log to https url
    logger = new HttpLogger(my_https_url, false);                // (initially disabled)
    logger.disable();                                            // disable logging for tests
    logger.enable();                                             // enable logging again
    if (logger.isEnabled()) ...                                  // test if logging is enabled

    // define request to log
    HttpServletRequest request = new HttpServletRequestImpl();
    request.setCharacterEncoding("UTF-8");
    request.setContentType("application/json");
    request.setHeader("A", "123");
    request.setMethod("GET");
    request.setRequestURL("http://google.com");

    // define response to log
    HttpServletResponse response = new HttpServletResponseImpl();
    response.setCharacterEncoding("UTF-8");
    response.setContentType("text/html");
    response.setHeader("B", "234");
    response.setStatus(200);

    // log objects defined above
    logger.log(request, null, response, null);

    // log with specified request/response bodies
    logger.log(request, "my-request, response, "my-response");

    // submit a custom message (destination may accept or not)
    logger.submit("...");
