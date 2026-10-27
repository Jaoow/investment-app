package dev.jaoow.investmentapp.infrastructure.web;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RequestObservabilityFilterTests {
    private final RequestObservabilityFilter filter = new RequestObservabilityFilter();
    private final Logger logger = (Logger) LoggerFactory.getLogger("investize.http.access");
    private final ListAppender<ILoggingEvent> events = new ListAppender<>();

    @BeforeEach
    void captureLogs() {
        events.start();
        logger.addAppender(events);
    }

    @AfterEach
    void cleanUp() {
        logger.detachAppender(events);
        events.stop();
        MDC.clear();
    }

    @Test
    void propagatesValidIdAndLogsOnlyRouteTemplate() throws Exception {
        String id = "A1234567-1234-1234-1234-123456789ABC";
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/public/portfolio-shares/private-token");
        request.addHeader(RequestObservabilityFilter.HEADER, id);
        request.addHeader("Authorization", "Bearer do-not-log");
        request.setQueryString("email=private@example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> {
            assertEquals(id.toLowerCase(), MDC.get("correlationId"));
            req.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/public/portfolio-shares/{token}");
            response.setStatus(404);
        });

        assertEquals(id.toLowerCase(), response.getHeader(RequestObservabilityFilter.HEADER));
        assertNull(MDC.get("correlationId"));
        String json = events.list.getFirst().getFormattedMessage();
        var event = new ObjectMapper().readTree(json);
        assertEquals(7, event.size());
        assertEquals("http_request", event.get("event").asText());
        assertEquals("GET", event.get("method").asText());
        assertEquals(id.toLowerCase(), event.get("correlationId").asText());
        assertDoesNotThrow(() -> java.time.Instant.parse(event.get("timestamp").asText()));
        assertEquals("/public/portfolio-shares/{token}", event.get("route").asText());
        assertEquals(404, event.get("status").asInt());
        assertTrue(event.get("durationMs").asLong() >= 0);
        assertFalse(json.contains("private-token"));
        assertFalse(json.contains("do-not-log"));
        assertFalse(json.contains("private@example.com"));
    }

    @Test
    void replacesInvalidIdsAndDoesNotLeakContextToNextRequest() throws Exception {
        for (String invalid : new String[]{"", "user@example.com", "x".repeat(200), "bad\r\nid"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/unknown/private-value");
            request.addHeader(RequestObservabilityFilter.HEADER, invalid);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> {});
            String id = response.getHeader(RequestObservabilityFilter.HEADER);
            assertNotNull(id);
            assertDoesNotThrow(() -> UUID.fromString(id));
            assertNotEquals(invalid, id);
            assertNull(MDC.get("correlationId"));
        }
        assertEquals(4, events.list.size());
        assertTrue(events.list.stream().allMatch(event -> event.getFormattedMessage().contains("\"route\":\"UNMAPPED\"")));
    }

    @Test
    void logsUnhandledFailureAndRestoresPreviousContextWithoutSwallowingException() {
        MDC.put("correlationId", "outer-context");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/failure");
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertThrows(ServletException.class, () -> filter.doFilter(request, response,
                (req, res) -> { throw new ServletException("expected failure"); }));

        assertEquals("outer-context", MDC.get("correlationId"));
        assertEquals(1, events.list.size());
        assertTrue(events.list.getFirst().getFormattedMessage().contains("\"status\":500"));
    }
}
