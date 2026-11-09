package com.naveenmandal.urlshortener.interceptor;

import com.naveenmandal.urlshortener.service.RateLimiterService;
import com.naveenmandal.urlshortener.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;

    // Strict limit for creating URLs: 10 requests / 60 seconds
    private static final int SHORTEN_LIMIT = 10;
    // Relaxed limit for redirecting / reading: 100 requests / 60 seconds
    private static final int REDIRECT_LIMIT = 100;
    private static final int WINDOW_SECONDS = 60;

    public RateLimitInterceptor(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = IpUtil.getClientIp(request);
        String uri = request.getRequestURI();
        String method = request.getMethod();

        String rateLimitKey;
        int limit;

        if ("POST".equalsIgnoreCase(method) && uri.startsWith("/shorten")) {
            // Strict limit for URL creation
            rateLimitKey = "rate:shorten:" + clientIp;
            limit = SHORTEN_LIMIT;
        } else if ("GET".equalsIgnoreCase(method) && !uri.contains("/stats")) {
            // Loose limit for redirects
            rateLimitKey = "rate:redirect:" + clientIp;
            limit = REDIRECT_LIMIT;
        } else {
            // Stats endpoint or other requests pass through
            return true;
        }

        boolean allowed = rateLimiterService.isAllowed(rateLimitKey, limit, WINDOW_SECONDS);

        if (!allowed) {
            long retryAfter = rateLimiterService.getRemainingTtl(rateLimitKey);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // HTTP 429
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                    "{\"error\": \"Too many requests\", \"message\": \"Rate limit exceeded. Try again in %d seconds.\"}",
                    retryAfter
            ));
            return false; // Stop the request chain, do not call controller!
        }

        return true; // Request allowed to proceed to controller
    }
//
//    /**
//     * Extracts the real client IP even if behind Nginx, Cloudflare, or AWS ALB.
//     */
//    private String getClientIp(HttpServletRequest request) {
//        String xForwardedFor = request.getHeader("X-Forwarded-For");
//        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
//            // If multiple proxies, first IP in the list is the actual client
//            return xForwardedFor.split(",")[0].trim();
//        }
//        return request.getRemoteAddr();
//    }
}