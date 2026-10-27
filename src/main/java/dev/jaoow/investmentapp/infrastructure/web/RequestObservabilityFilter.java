package dev.jaoow.investmentapp.infrastructure.web;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestObservabilityFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Correlation-ID";
    private static final String MDC_KEY = "correlationId";
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Logger ACCESS_LOG = LoggerFactory.getLogger("investize.http.access");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String suppliedId = request.getHeader(HEADER);
        String correlationId = suppliedId != null && suppliedId.length() == 36
                && UUID_PATTERN.matcher(suppliedId).matches()
                ? suppliedId.toLowerCase(java.util.Locale.ROOT) : UUID.randomUUID().toString();
        String previousId = MDC.get(MDC_KEY);
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        long startedAt = System.nanoTime();
        boolean completed = false;
        try {
            chain.doFilter(request, response);
            completed = true;
        } finally {
            try {
                Object route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
                var event = JsonNodeFactory.instance.objectNode()
                        .put("timestamp", java.time.Instant.now().toString())
                        .put("event", "http_request")
                        .put("correlationId", correlationId)
                        .put("method", request.getMethod())
                        .put("route", route == null ? "UNMAPPED" : route.toString())
                        .put("status", completed ? response.getStatus() : 500)
                        .put("durationMs", (System.nanoTime() - startedAt) / 1_000_000);
                ACCESS_LOG.info("{}", event);
            } finally {
                if (previousId == null) MDC.remove(MDC_KEY);
                else MDC.put(MDC_KEY, previousId);
            }
        }
    }
}
