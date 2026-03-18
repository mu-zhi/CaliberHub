package com.cmbchina.datadirect.caliber.infrastructure.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.UUID;

public final class RequestTraceContext {

    public static final String ATTR_REQUEST_ID = "caliber.requestId";
    public static final String ATTR_ERROR_CODE = "caliber.errorCode";
    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    public static final String HEADER_ERROR_CODE = "X-Error-Code";

    private RequestTraceContext() {
    }

    public static String ensureRequestId(HttpServletRequest request, HttpServletResponse response) {
        Object value = request.getAttribute(ATTR_REQUEST_ID);
        if (value instanceof String text && !text.isBlank()) {
            response.setHeader(HEADER_REQUEST_ID, text);
            return text;
        }
        String fromHeader = trim(request.getHeader(HEADER_REQUEST_ID));
        String requestId = fromHeader.isEmpty() ? newRequestId() : fromHeader;
        request.setAttribute(ATTR_REQUEST_ID, requestId);
        response.setHeader(HEADER_REQUEST_ID, requestId);
        return requestId;
    }

    public static String currentRequestId(HttpServletRequest request) {
        Object value = request.getAttribute(ATTR_REQUEST_ID);
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        String fromHeader = trim(request.getHeader(HEADER_REQUEST_ID));
        return fromHeader.isEmpty() ? "-" : fromHeader;
    }

    public static void markErrorCode(HttpServletRequest request, HttpServletResponse response, String code) {
        String safeCode = trim(code);
        if (safeCode.isEmpty()) {
            return;
        }
        request.setAttribute(ATTR_ERROR_CODE, safeCode);
        response.setHeader(HEADER_ERROR_CODE, safeCode);
    }

    private static String newRequestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}

