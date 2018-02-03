// © 2016-2018 Resurface Labs LLC

package io.resurface;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Parser and utilities for HTTP logger rules.
 */
public class HttpRules {

    /**
     * Rules used by default when no other rules are provided.
     */
    public static String getStandardRules() {
        return "/request_header:cookie|response_header:set-cookie/ remove\n" +
                "/(request|response)_body|request_param/ replace /[a-zA-Z0-9.!#$%&’*+\\/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)/, /x@y.com/\n" +
                "/request_body|request_param|response_body/ replace /[0-9\\.\\-\\/]{9,}/, /xyxy/\n";
    }

    /**
     * Rules providing all details for debugging an application.
     */
    public static String getDebugRules() {
        return "allow_http_url\ncopy_session_field /.*/\n";
    }

    /**
     * Rules providing details for a traditional weblog.
     */
    public static String getWeblogRules() {
        return "/request_url/ replace /([^\\?;]+).*/, /$1/\n" +
                "/request_body|response_body|request_param:.*|request_header:(?!user-agent).*|response_header:(?!(content-length)|(content-type)).*/ remove\n";
    }

    /**
     * Parses rules from multi-line string.
     */
    public static List<HttpRule> parse(String rules) {
        List<HttpRule> result = new ArrayList<>();
        if (rules != null) {
            rules = rules.replaceAll("(?m)^\\s*include debug\\s*$", Matcher.quoteReplacement(getDebugRules()));
            rules = rules.replaceAll("(?m)^\\s*include standard\\s*$", Matcher.quoteReplacement(getStandardRules()));
            rules = rules.replaceAll("(?m)^\\s*include weblog\\s*$", Matcher.quoteReplacement(getWeblogRules()));
            for (String rule : rules.split("\\r?\\n")) {
                HttpRule parsed = parseRule(rule);
                if (parsed != null) result.add(parsed);
            }
        }
        return result;
    }

    /**
     * Parses rule from single line.
     */
    public static HttpRule parseRule(String r) {
        if ((r == null) || REGEX_BLANK_OR_COMMENT.matcher(r).matches()) return null;
        Matcher m = REGEX_ALLOW_HTTP_URL.matcher(r);
        if (m.matches()) return new HttpRule("allow_http_url", null, null, null);
        m = REGEX_COPY_SESSION_FIELD.matcher(r);
        if (m.matches()) return new HttpRule("copy_session_field", null, parseRegex(r, m.group(1)), null);
        m = REGEX_REMOVE.matcher(r);
        if (m.matches()) return new HttpRule("remove", parseRegex(r, m.group(1)), null, null);
        m = REGEX_REMOVE_IF.matcher(r);
        if (m.matches()) return new HttpRule("remove_if", parseRegex(r, m.group(1)), parseRegex(r, m.group(2)), null);
        m = REGEX_REMOVE_IF_FOUND.matcher(r);
        if (m.matches()) return new HttpRule("remove_if_found", parseRegex(r, m.group(1)), parseRegexFind(r, m.group(2)), null);
        m = REGEX_REMOVE_UNLESS.matcher(r);
        if (m.matches()) return new HttpRule("remove_unless", parseRegex(r, m.group(1)), parseRegex(r, m.group(2)), null);
        m = REGEX_REMOVE_UNLESS_FOUND.matcher(r);
        if (m.matches())
            return new HttpRule("remove_unless_found", parseRegex(r, m.group(1)), parseRegexFind(r, m.group(2)), null);
        m = REGEX_REPLACE.matcher(r);
        if (m.matches())
            return new HttpRule("replace", parseRegex(r, m.group(1)), parseRegexFind(r, m.group(2)), parseString(r, m.group(3)));
        m = REGEX_SAMPLE.matcher(r);
        if (m.matches()) {
            Integer m1 = Integer.valueOf(m.group(1));
            if (m1 < 1 || m1 > 99) throw new IllegalArgumentException(String.format("Invalid sample percent: %d", m1));
            return new HttpRule("sample", null, m1, null);
        }
        m = REGEX_SKIP_COMPRESSION.matcher(r);
        if (m.matches()) return new HttpRule("skip_compression", null, null, null);
        m = REGEX_SKIP_SUBMISSION.matcher(r);
        if (m.matches()) return new HttpRule("skip_submission", null, null, null);
        m = REGEX_STOP.matcher(r);
        if (m.matches()) return new HttpRule("stop", parseRegex(r, m.group(1)), null, null);
        m = REGEX_STOP_IF.matcher(r);
        if (m.matches()) return new HttpRule("stop_if", parseRegex(r, m.group(1)), parseRegex(r, m.group(2)), null);
        m = REGEX_STOP_IF_FOUND.matcher(r);
        if (m.matches()) return new HttpRule("stop_if_found", parseRegex(r, m.group(1)), parseRegexFind(r, m.group(2)), null);
        m = REGEX_STOP_UNLESS.matcher(r);
        if (m.matches()) return new HttpRule("stop_unless", parseRegex(r, m.group(1)), parseRegex(r, m.group(2)), null);
        m = REGEX_STOP_UNLESS_FOUND.matcher(r);
        if (m.matches()) return new HttpRule("stop_unless_found", parseRegex(r, m.group(1)), parseRegexFind(r, m.group(2)), null);
        throw new IllegalArgumentException(String.format("Invalid rule: %s", r));
    }

    /**
     * Parses regex for matching.
     */
    private static Pattern parseRegex(String r, String regex) {
        String str = parseString(r, regex);
        if ("*".equals(str) || "+".equals(str) || "?".equals(str))
            throw new IllegalArgumentException(String.format("Invalid regex (%s) in rule: %s", regex, r));
        if (!str.startsWith("^")) str = "^" + str;
        if (!str.endsWith("$")) str = str + "$";
        try {
            return Pattern.compile(str);
        } catch (PatternSyntaxException pse) {
            throw new IllegalArgumentException(String.format("Invalid regex (%s) in rule: %s", regex, r));
        }
    }

    /**
     * Parses regex for finding.
     */
    private static Pattern parseRegexFind(String r, String regex) {
        try {
            return Pattern.compile(parseString(r, regex));
        } catch (PatternSyntaxException pse) {
            throw new IllegalArgumentException(String.format("Invalid regex (%s) in rule: %s", regex, r));
        }
    }

    /**
     * Parses delimited string expression.
     */
    private static String parseString(String r, String str) {
        for (String sep : new String[]{"~", "!", "%", "|", "/"}) {
            Matcher m = Pattern.compile(String.format("^[%s](.*)[%s]$", sep, sep)).matcher(str);
            if (m.matches()) {
                String m1 = m.group(1);
                if (Pattern.compile(String.format("^[%s].*|.*[^\\\\][%s].*", sep, sep)).matcher(m1).matches())
                    throw new IllegalArgumentException(String.format("Unescaped separator (%s) in rule: %s", sep, r));
                return m1.replace("\\" + sep, sep);
            }
        }
        throw new IllegalArgumentException(String.format("Invalid expression (%s) in rule: %s", str, r));
    }

    private static final Pattern REGEX_ALLOW_HTTP_URL = Pattern.compile("^\\s*allow_http_url\\s*(#.*)?$");
    private static final Pattern REGEX_BLANK_OR_COMMENT = Pattern.compile("^\\s*([#].*)*$");
    private static final Pattern REGEX_COPY_SESSION_FIELD = Pattern.compile("^\\s*copy_session_field\\s+([~!%|/].+[~!%|/])\\s*(#.*)?$");
    private static final Pattern REGEX_REMOVE = Pattern.compile("^\\s*([~!%|/].+[~!%|/])\\s*remove\\s*(#.*)?$");
    private static final Pattern REGEX_REMOVE_IF = Pattern.compile("^\\s*([~!%|/].+[~!%|/])\\s*remove_if\\s+([~!%|/].+[~!%|/])\\s*(#.*)?$");
    private static final Pattern REGEX_REMOVE_IF_FOUND = Pattern.compile("^\\s*([~!%|/].+[~!%|/])\\s*remove_if_found\\s+([~!%|/].+[~!%|/])\\s*(#.*)?$");
    private static final Pattern REGEX_REMOVE_UNLESS = Pattern.compile("^\\s*([~!%|/].+[~!%|/])\\s*remove_unless\\s+([~!%|/].+[~!%|/])\\s*(#.*)?$");
    private static final Pattern REGEX_REMOVE_UNLESS_FOUND = Pattern.compile("^\\s*([~!%|/].+[~!%|/])\\s*remove_unless_found\\s+([~!%|/].+[~!%|/])\\s*(#.*)?$");
    private static final Pattern REGEX_REPLACE = Pattern.compile("^\\s*([~!%|/].+[~!%|/])\\s*replace[\\s]+([~!%|/].+[~!%|/]),[\\s]+([~!%|/].*[~!%|/])\\s*(#.*)?$");
    private static final Pattern REGEX_SAMPLE = Pattern.compile("^\\s*sample\\s+(\\d+)\\s*(#.*)?$");
    private static final Pattern REGEX_SKIP_COMPRESSION = Pattern.compile("^\\s*skip_compression\\s*(#.*)?$");
    private static final Pattern REGEX_SKIP_SUBMISSION = Pattern.compile("^\\s*skip_submission\\s*(#.*)?$");
    private static final Pattern REGEX_STOP = Pattern.compile("^\\s*([~!%|/].+[~!%|/])\\s*stop\\s*(#.*)?$");
    private static final Pattern REGEX_STOP_IF = Pattern.compile("^\\s*([~!%|/].+[~!%|/])\\s*stop_if\\s+([~!%|/].+[~!%|/])\\s*(#.*)?$");
    private static final Pattern REGEX_STOP_IF_FOUND = Pattern.compile("^\\s*([~!%|/].+[~!%|/])\\s*stop_if_found\\s+([~!%|/].+[~!%|/])\\s*(#.*)?$");
    private static final Pattern REGEX_STOP_UNLESS = Pattern.compile("^\\s*([~!%|/].+[~!%|/])\\s*stop_unless\\s+([~!%|/].+[~!%|/])\\s*(#.*)?$");
    private static final Pattern REGEX_STOP_UNLESS_FOUND = Pattern.compile("^\\s*([~!%|/].+[~!%|/])\\s*stop_unless_found\\s+([~!%|/].+[~!%|/])\\s*(#.*)?$");

}