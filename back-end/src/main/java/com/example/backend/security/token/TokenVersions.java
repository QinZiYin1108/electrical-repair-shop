package com.example.backend.security.token;

public final class TokenVersions {

    private TokenVersions() {}

    public static int current(Integer version) {
        return version == null || version < 1 ? 1 : version;
    }

    public static int next(Integer version) {
        return current(version) + 1;
    }
}
