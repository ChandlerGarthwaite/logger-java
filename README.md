# resurfaceio-logger-java
&copy; 2016-2018 Resurface Labs LLC

This library makes it easy to log actual usage of Java web/json apps.

## Contents

<ul>
<li><a href="#dependencies">Dependencies</a></li>
<li><a href="#installing_with_maven">Installing With Maven</a></li>
<li><a href="#logging_from_servlet_filter">Logging From Servlet Filter</a></li>
<li><a href="#logging_from_spring_boot">Logging From Spring Boot</a></li>
<li><a href="#logging_from_spark_framework">Logging From Spark Framework</a></li>
<li><a href="#advanced_topics">Advanced Topics</a><ul>
<li><a href="#setting_default_url">Setting Default URL</a></li>
<li><a href="#enabling_and_disabling">Enabling and Disabling Loggers</a></li>
<li><a href="#logging_api">Logging API</a></li>
</ul></li>
</ul>

<a name="dependencies"/>

## Dependencies

Requires Java 8. No other dependencies to conflict with your app.

<a name="installing_with_maven"/>

## Installing with Maven

Add this section to `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>io.resurface</groupId>
        <artifactId>resurfaceio-logger</artifactId>
        <version>RELEASE</version>
    </dependency>
</dependencies>
```

<a name="logging_from_servlet_filter"/>

## Logging From Servlet Filter

After <a href="#installing_with_maven">installing the library</a>, add a logging filter to `web.xml`.

```xml
<filter>
    <filter-name>HttpLoggerForServlets</filter-name>
    <filter-class>io.resurface.HttpLoggerForServlets</filter-class>
    <init-param>
        <param-name>url</param-name>
        <param-value>https://my-logging-url</param-value>
    </init-param>
</filter>
<filter-mapping>
    <filter-name>HttpLoggerForServlets</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```

<a name="logging_from_spring_boot"/>

## Logging From Spring Boot

After <a href="#installing_with_maven">installing the library</a>, configure a `FilterRegistrationBean`
to add a logging servlet filter.

```java
@Bean
public FilterRegistrationBean httpLoggerFilter() {
    FilterRegistrationBean frb = new FilterRegistrationBean();
    frb.setFilter(new io.resurface.HttpLoggerForServlets());
    frb.addInitParameter("url", "https://my-logging-url");
    frb.addUrlPatterns("/*");
    frb.setName("HttpLoggerForServlets");
    return frb;
}
```

<a name="logging_from_spark_framework"/>

## Logging From Spark Framework

After <a href="#installing_with_maven">installing the library</a>, create a logger and call it from the routes of interest.

```java
import io.resurface.HttpLogger;

HttpLogger logger = new HttpLogger("https://my-logging-url");

get("/hello", (request, response) -> {
    String response_body = "Hello World";
    logger.log(request.raw(), response.raw(), response_body);
    return response_body;
});

post("/hello_post", (request, response) -> {
    String response_body = "POSTED: " + request.body();
    logger.log(request.raw(), response.raw(), response_body, request.body());
    return response_body;
});
```

Alternatively configure an `after` filter to log across multiple routes at once.

```java
after((request, response) -> {
    if (response.body() != null) {  // log successful responses only, not 404/500s
        logger.log(request.raw(), response.raw(), response.body(), request.body());
    }
});
```

<a name="advanced_topics"/>

## Advanced Topics

<a name="setting_default_url"/>

### Setting Default URL

Set the `USAGE_LOGGERS_URL` variable to provide a default value whenever the URL is not specified.

```java
// from command line
export USAGE_LOGGERS_URL="https://my-logging-url"

// when launching Java app
java -DUSAGE_LOGGERS_URL="https://my-logging-url" ...

// for Heroku app
heroku config:set USAGE_LOGGERS_URL=https://my-logging-url
```

Loggers look for this environment variable when no URL is provided.

```java
// for basic logger
HttpLogger logger = new HttpLogger();

// for servlet filter
<filter>
    <filter-name>HttpLoggerForServlets</filter-name>
    <filter-class>io.resurface.HttpLoggerForServlets</filter-class>
</filter>
```

<a name="enabling_and_disabling"/>

### Enabling and Disabling Loggers

Individual loggers can be controlled through their `enable` and `disable` methods. When disabled, loggers will
not send any logging data, and the result returned by the `log` method will always be true (success).

All loggers for an application can be enabled or disabled at once with the `UsageLoggers` class. This even controls
loggers that have not yet been created by the application.

```java
UsageLoggers.disable();    // disable all loggers
UsageLoggers.enable();     // enable all loggers
```

All loggers can be permanently disabled with the `USAGE_LOGGERS_DISABLE` environment variable. When set to true,
loggers will never become enabled, even if `UsageLoggers.enable()` is called by the application. This is primarily 
done by automated tests to disable all logging even if other control logic exists. 

```java
// from command line
export USAGE_LOGGERS_DISABLE="true"

// when launching Java app
java -DUSAGE_LOGGERS_DISABLE="true" ...

// for Heroku app
heroku config:set USAGE_LOGGERS_DISABLE=true
```

<a name="logging_api"/>

### Logging API

Loggers can be directly integrated into your application with this API, which gives complete control over how
usage logging is implemented.

```java
import io.resurface.*;

// create and configure logger
HttpLogger logger;
logger = new HttpLogger(my_https_url);                           // log to remote url
logger = new HttpLogger(my_https_url, false);                    // (initially disabled)
logger = new HttpLogger(queue);                                  // log to appendable queue
logger = new HttpLogger(queue, false);                           // (initially disabled)
logger.disable();                                                // disable logging for tests
logger.enable();                                                 // enable logging again
if (logger.isEnabled()) ...                                      // test if logging is enabled

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
logger.log(request, response);

// log with specified request/response bodies
logger.log(request, response, "my-response-body", "my-request-body");

// submit a custom message (destination may accept or not)
logger.submit("...");
```
