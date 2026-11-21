package com.sajha.backend.phone;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class LocalOtpVerifyClientTest {
    private MockEnvironment local() {
        var env = new MockEnvironment()
            .withProperty("server.address", "127.0.0.1")
            .withProperty("spring.datasource.url", "jdbc:mysql://127.0.0.1:3307/sajha?useUnicode=true");
        env.setActiveProfiles("local");
        return env;
    }

    @Test void allowsOnlyLocalBoundServerAndDatabase() {
        assertDoesNotThrow(() -> new LocalOtpVerifyClient(local()));
        var publicServer = local().withProperty("server.address", "0.0.0.0");
        assertThrows(IllegalStateException.class, () -> new LocalOtpVerifyClient(publicServer));
        var remoteDatabase = local().withProperty("spring.datasource.url",
            "jdbc:mysql://db.example.com:3306/sajha");
        assertThrows(IllegalStateException.class, () -> new LocalOtpVerifyClient(remoteDatabase));
    }

    @Test void localProviderIssuesCodeButRejectsNonNumericGuess() {
        var provider = new LocalOtpVerifyClient(local());
        provider.send("+12025550142");
        assertFalse(provider.check("+12025550142", "not-a-code"));
        assertFalse(provider.check("+12025550143", "123456"));
    }
}
