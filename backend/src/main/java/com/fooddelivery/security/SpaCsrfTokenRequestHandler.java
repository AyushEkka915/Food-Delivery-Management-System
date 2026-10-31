package com.fooddelivery.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.Supplier;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

/** Keeps XOR masking for request attributes while accepting the raw cookie value in the SPA header. */
public final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
    public static final String BOOTSTRAP_TOKEN_ATTRIBUTE = "com.fooddelivery.security.csrfBootstrapToken";

    private final XorCsrfTokenRequestAttributeHandler xorHandler = new XorCsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestAttributeHandler plainHandler = new CsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       Supplier<CsrfToken> deferredCsrfToken) {
        xorHandler.handle(request, response, deferredCsrfToken);
        CsrfToken csrfToken = deferredCsrfToken.get();
        request.setAttribute(BOOTSTRAP_TOKEN_ATTRIBUTE, csrfToken.getToken());
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        String headerValue = request.getHeader(csrfToken.getHeaderName());
        return (StringUtils.hasText(headerValue) ? plainHandler : xorHandler)
                .resolveCsrfTokenValue(request, csrfToken);
    }
}
