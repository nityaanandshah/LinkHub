package com.linkhub.analytics.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ua_parser.Client;
import ua_parser.Parser;

/**
 * Service to parse User-Agent strings into device type, browser, and OS.
 * Uses the ua-parser library for reliable parsing.
 */
@Service
public class UserAgentParser {

    private static final Logger log = LoggerFactory.getLogger(UserAgentParser.class);

    private final Parser parser;

    public UserAgentParser() {
        this.parser = new Parser();
    }

    /**
     * Parse a User-Agent string into structured data.
     */
    public ParsedUserAgent parse(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return new ParsedUserAgent("Unknown", "Unknown", "Unknown");
        }

        try {
            Client client = parser.parse(userAgent);

            String browser = client.userAgent != null && client.userAgent.family != null
                    ? client.userAgent.family
                    : "Unknown";

            String os = client.os != null && client.os.family != null
                    ? client.os.family
                    : "Unknown";

            String deviceType = inferDeviceType(userAgent, os);

            return new ParsedUserAgent(deviceType, browser, os);
        } catch (Exception e) {
            log.debug("Failed to parse User-Agent: {}", e.getMessage());
            return new ParsedUserAgent("Unknown", "Unknown", "Unknown");
        }
    }

    /**
     * Infer device type from the user agent string and OS.
     */
    private String inferDeviceType(String userAgent, String os) {
        String ua = userAgent.toLowerCase();

        if (ua.contains("bot") || ua.contains("crawler") || ua.contains("spider")) {
            return "Bot";
        }
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            return "Mobile";
        }
        if (ua.contains("tablet") || ua.contains("ipad")) {
            return "Tablet";
        }
        if ("iOS".equals(os) || "Android".equals(os)) {
            return "Mobile";
        }
        if (ua.contains("windows") || ua.contains("macintosh") || ua.contains("linux")) {
            return "Desktop";
        }

        return "Other";
    }

    /**
     * Parsed User-Agent result record.
     */
    public record ParsedUserAgent(String deviceType, String browser, String os) {}
}
