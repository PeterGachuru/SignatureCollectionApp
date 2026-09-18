package ke.co.signature.Utils;


import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

import org.slf4j.Logger;
import org.springframework.web.util.ContentCachingRequestWrapper;


@Component
public class RequestLoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void doFilter(jakarta.servlet.ServletRequest request,
                         jakarta.servlet.ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;

        // Wrap request to allow multiple reads of the body
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(req);

        chain.doFilter(wrappedRequest, response);

        // Log request details
        logRequest(wrappedRequest);
    }

    private void logRequest(ContentCachingRequestWrapper request) throws IOException {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();

        String payload = new String(request.getContentAsByteArray(), request.getCharacterEncoding());

        logger.info("=== Incoming Request ===");
        logger.info("Method: {}", method);
        logger.info("URI: {}{}", uri, (query != null ? "?" + query : ""));
        logger.info("Headers:");
        request.getHeaderNames().asIterator()
                .forEachRemaining(name -> logger.info("{}: {}", name, request.getHeader(name)));
        if (!payload.isBlank()) {
            logger.info("Payload:\n{}", payload);
        }
        logger.info("=======================");
    }
}
