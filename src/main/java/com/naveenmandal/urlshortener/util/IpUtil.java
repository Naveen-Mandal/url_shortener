package com.naveenmandal.urlshortener.util;

import jakarta.servlet.http.HttpServletRequest;

public class IpUtil {

    private IpUtil() {} // Private constructor so nobody instantiates it

    public static String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}