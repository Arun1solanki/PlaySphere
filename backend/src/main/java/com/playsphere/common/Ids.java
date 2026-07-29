package com.playsphere.common;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.UUID;

public final class Ids {
    private static final SecureRandom RANDOM = new SecureRandom();
    private Ids() {}
    public static String uuid() { return UUID.randomUUID().toString(); }
    public static String code(String prefix) {
        byte[] bytes = new byte[5];
        RANDOM.nextBytes(bytes);
        return prefix + "-" + HexFormat.of().formatHex(bytes).toUpperCase();
    }
    public static String token() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
