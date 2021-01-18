package org.jboss.aerogear.unifiedpush.rest;

import org.jboss.aerogear.unifiedpush.service.impl.spring.OAuth2Configuration;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AbstractEndpoint {
    // Valid origin can only be "https://cb4/" or "https://*-mcs.c-b4.com/*"
    // (Super-Pharm is an exception due to its existence in production)
    private static List<Pattern> originPatterns =
            convertRegexStringsToPatterns(
                    Stream.concat(Arrays.asList("^https://cb4/",
                                        "^https://(([a-zA-Z0-9-]+)(-|.))?mcs.c-b4.(com|info)/?([a-zA-Z0-9./]*)").stream(),
                            OAuth2Configuration.getStaticCorsValidOrigins().stream())
                            .collect(Collectors.toList()));

    private static List<Pattern> convertRegexStringsToPatterns(List<String> regexStrings) {
        List<Pattern> patterns = new ArrayList<Pattern>();
        for(String rs: regexStrings) {
            patterns.add(Pattern.compile(rs));
        }
        return patterns;
    }

    private static String returnIfValidOrigin(String originUrl) {
        if(originUrl!=null && !originUrl.isEmpty()) {
            for(Pattern pattern: originPatterns) {
                if(pattern.matcher(originUrl).matches())
                    return originUrl;
            }
        }
        return null;  // An null origin should be dropped
    }

	protected static ResponseBuilder appendPreflightResponseHeaders(HttpHeaders headers, ResponseBuilder response) {
        // add response headers for the preflight request
        // required
        String acaoUrl = returnIfValidOrigin(headers.getRequestHeader("Origin").get(0));
        response.header("Access-Control-Allow-Origin", acaoUrl) // return submitted origin
                .header("Vary",(acaoUrl!=null ? "Origin" : null))
                .header("Access-Control-Allow-Methods", "POST, DELETE, PUT, GET, OPTIONS")
                .header("Access-Control-Allow-Headers", "accept, origin, content-type, authorization, device-token") // explicit Headers!
                .header("Access-Control-Allow-Credentials", "true")
                // indicates how long the results of a preflight request can be cached (in seconds)
                .header("Access-Control-Max-Age", "604800"); // for now, we keep it for seven days

        return response;
    }

	protected Response appendAllowOriginHeader(ResponseBuilder rb, HttpServletRequest request) {
        String acaoUrl = returnIfValidOrigin(request.getHeader("Origin"));
        return rb.header("Access-Control-Allow-Origin", acaoUrl) // return submitted origin
                .header("Vary",(acaoUrl!=null ? "Origin" : null))
                .header("Access-Control-Allow-Credentials", "true")
				.build();
    }

	protected Response create401Response(final HttpServletRequest request) {
        return appendAllowOriginHeader(
                Response.status(Status.UNAUTHORIZED)
                        .header("WWW-Authenticate", "Basic realm=\"AeroGear UnifiedPush Server\"")
                        .entity(quote("Unauthorized Request")),
                request);
    }

    // Append double quotes to strings, used to overcome jax-rs issue with simple stings.
    // http://stackoverflow.com/questions/7705081/jax-rs-resteasy-service-return-json-string-without-double-quote
    protected static String quote(String value) {
    	return new StringBuilder(value.length() + 2).append('"' + value + '"').toString();
    }
}
