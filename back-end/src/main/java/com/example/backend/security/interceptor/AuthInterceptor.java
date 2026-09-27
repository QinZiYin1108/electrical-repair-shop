package com.example.backend.security.interceptor;

import com.example.backend.common.ErrorCode;
import com.example.backend.entity.AdminAccounts;
import com.example.backend.entity.TechnicianAccounts;
import com.example.backend.entity.UserAccounts;
import com.example.backend.exception.BusinessException;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.security.token.TokenService;
import com.example.backend.service.AdminAccountsService;
import com.example.backend.service.TechnicianAccountsService;
import com.example.backend.service.UserAccountsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String PUBLIC_MALL_PRODUCT_PREFIX = "/user/mall/products/";

    private final TokenService tokenService;
    private final UserAccountsService userAccountsService;
    private final TechnicianAccountsService technicianAccountsService;
    private final AdminAccountsService adminAccountsService;

    public AuthInterceptor(
            TokenService tokenService,
            UserAccountsService userAccountsService,
            TechnicianAccountsService technicianAccountsService,
            AdminAccountsService adminAccountsService) {
        this.tokenService = tokenService;
        this.userAccountsService = userAccountsService;
        this.technicianAccountsService = technicianAccountsService;
        this.adminAccountsService = adminAccountsService;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
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
        boolean phoneBound = checkAccountExists(userInfo);
        if (!phoneBound && userInfo.getRole() == AccountRole.USER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "请先绑定手机号");
        }
        AuthUserContext.set(userInfo);
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {
        AuthUserContext.clear();
    }

    private boolean requiresAuth(String uri) {
        if (uri == null || uri.contains("/pass/")) {
            return false;
        }
        return uri.contains("/admin/")
                || uri.contains("/worker/")
                || uri.contains("/user/")
                || uri.contains("/common/");
    }

    private boolean isPublicMallBrowseRequest(HttpServletRequest request, String uri) {
        if (!"GET".equalsIgnoreCase(request.getMethod()) || uri == null) {
            return false;
        }
        if ("/user/mall/categories".equals(uri)
                || "/user/mall/products".equals(uri)
                || "/user/mall/products/{id}".equals(uri)) {
            return true;
        }
        return uri.startsWith(PUBLIC_MALL_PRODUCT_PREFIX)
                && uri.indexOf('/', PUBLIC_MALL_PRODUCT_PREFIX.length()) < 0;
    }

    /**
     * @return true if phone is bound (or non-USER role); false if USER with no phone
     */
    private boolean checkAccountExists(LoginUserInfo userInfo) {
        String accountId = userInfo.getAccountId();
        if (accountId == null || accountId.isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号不存在或已注销，请重新登录");
        }

        boolean exists = false;
        boolean phoneBound = true;
        int currentTokenVersion = 0;
        switch (userInfo.getRole()) {
            case USER -> {
                UserAccounts account = userAccountsService.getById(accountId);
                exists = account != null;
                if (exists) {
                    phoneBound = account.getPhone() != null && !account.getPhone().isEmpty();
                    currentTokenVersion = normalizeVersion(account.getTokenVersion());
                    if (!Integer.valueOf(1).equals(account.getStatus())) {
                        throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号状态已变更，请重新登录");
                    }
                }
            }
            case WORKER -> {
                TechnicianAccounts account = technicianAccountsService.getById(accountId);
                exists = account != null;
                if (exists) {
                    currentTokenVersion = normalizeVersion(account.getTokenVersion());
                    int status =
                            account.getAccountStatus() == null ? 0 : account.getAccountStatus();
                    if (status == 3 || status == 4) {
                        throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号状态已变更，请重新登录");
                    }
                }
            }
            case ADMIN -> {
                AdminAccounts account = adminAccountsService.getById(accountId);
                exists = account != null;
                if (exists) {
                    currentTokenVersion = normalizeVersion(account.getTokenVersion());
                    if (!Integer.valueOf(1).equals(account.getAccountStatus())) {
                        throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号状态已变更，请重新登录");
                    }
                    userInfo.setAdminRole(
                            account.getAdminRole() == null ? 1 : account.getAdminRole());
                }
            }
        }
        if (!exists) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号不存在或已注销，请重新登录");
        }
        if (userInfo.getTokenVersion() == null
                || userInfo.getTokenVersion() != currentTokenVersion) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录状态已失效，请重新登录");
        }
        return phoneBound;
    }

    private int normalizeVersion(Integer version) {
        return version == null || version < 1 ? 1 : version;
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
