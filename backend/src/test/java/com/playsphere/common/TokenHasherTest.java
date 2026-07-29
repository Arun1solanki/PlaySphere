package com.playsphere.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenHasherTest {
    private final TokenHasher hasher = new TokenHasher();

    @Test
    void generatedTokensAreRandomAndHashesAreStable() {
        String first = hasher.newRawToken();
        String second = hasher.newRawToken();

        assertThat(first).isNotEqualTo(second);
        assertThat(hasher.sha256(first)).hasSize(64);
        assertThat(hasher.sha256(first)).isEqualTo(hasher.sha256(first));
    }
}
