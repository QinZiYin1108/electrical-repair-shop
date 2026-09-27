package com.example.backend.security.interceptor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.backend.entity.UserAccounts;
import com.example.backend.exception.BusinessException;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.security.token.TokenService;
import com.example.backend.service.AdminAccountsService;
import com.example.backend.service.TechnicianAccountsService;
import com.example.backend.service.UserAccountsService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthInterceptorTests {

    @Test
    void rejectsTokenAfterAccountVersionChanges() {
        TokenService tokenService = mock(TokenService.class);
        UserAccountsService userAccountsService = mock(UserAccountsService.class);
        AuthInterceptor interceptor =
                new AuthInterceptor(
                        tokenService,
                        userAccountsService,
                        mock(TechnicianAccountsService.class),
                        mock(AdminAccountsService.class));

        LoginUserInfo tokenUser = new LoginUserInfo();
        tokenUser.setAccountId("U1");
        tokenUser.setRole(AccountRole.USER);
        tokenUser.setTokenVersion(1);
        when(tokenService.parseToken("old-token")).thenReturn(tokenUser);

        UserAccounts current = new UserAccounts();
        current.setId("U1");
        current.setPhone("13800138000");
        current.setStatus(1);
        current.setTokenVersion(2);
        when(userAccountsService.getById("U1")).thenReturn(current);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/user/profile");
        request.addHeader("Authorization", "Bearer old-token");

        assertThrows(
                BusinessException.class,
                () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }
}
