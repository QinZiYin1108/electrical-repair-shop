package com.example.backend.security.interceptor;

import com.example.backend.common.ErrorCode;
import com.example.backend.exception.BusinessException;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.security.token.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String PUBLIC_MALL_PRODUCT_PREFIX = "/user/mall/products/";

    private final TokenService tokenService;

    public AuthInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String uri = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (uri == null) {
            uri = request.getRequestURI();
        }
        if (!requiresAuth(uri)) {
            return true;
        }

        boolean allowGuestAccess = isPublicMallBrowseRequest(request, uri);
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isEmpty()) {
            if (allowGuestAccess) {
                return true;
            }
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }

        String token = authHeader;
        if (authHeader.toLowerCase().startsWith("bearer ")) {
            token = authHeader.substring(7);
        }
        LoginUserInfo userInfo = tokenService.parseToken(token);
        checkRole(userInfo.getRole(), uri);
        AuthUserContext.set(userInfo);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthUserContext.clear();
    }

    private boolean requiresAuth(String uri) {
        if (uri == null || uri.contains("/pass/")) {
            return false;
        }
        return uri.contains("/admin/") || uri.contains("/worker/") || uri.contains("/user/") || uri.contains("/common/");
    }

    private boolean isPublicMallBrowseRequest(HttpServletRequest request, String uri) {
        if (!"GET".equalsIgnoreCase(request.getMethod()) || uri == null) {
            return false;
        }
        if ("/user/mall/categories".equals(uri) || "/user/mall/products".equals(uri) || "/user/mall/products/{id}".equals(uri)) {
            return true;
        }
        return uri.startsWith(PUBLIC_MALL_PRODUCT_PREFIX)
            && uri.indexOf('/', PUBLIC_MALL_PRODUCT_PREFIX.length()) < 0;
    }

    private void checkRole(AccountRole role, String uri) {
        if (uri.contains("/admin/")) {
            if (role != AccountRole.ADMIN) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问管理员接口");
            }
            return;
        }
        if (uri.contains("/worker/")) {
            if (role != AccountRole.WORKER) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问师傅接口");
            }
            return;
        }
        if (uri.contains("/user/") && role != AccountRole.USER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问用户接口");
        }
    }
}
