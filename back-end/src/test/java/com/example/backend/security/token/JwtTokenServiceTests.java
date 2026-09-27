package com.example.backend.security.token;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.utils.jwt.JwtUtil;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTests {

    @Test
    void preservesTokenVersionClaim() {
        JwtTokenService service =
                new JwtTokenService(new JwtUtil("01234567890123456789012345678901", 3600));

        String token = service.generateToken("U1", AccountRole.USER, 7);
        LoginUserInfo parsed = service.parseToken(token);

        assertEquals(7, parsed.getTokenVersion());
    }
}
