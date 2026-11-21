package com.sajha.backend.phone;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Explicit LOCAL PROFILE ONLY testing provider. No SMS is transmitted.
 * Startup refuses non-loopback server binding or any nonlocal database.
 * Never deploy with the 'local' Spring profile.
 */
@Component
@Profile("local")
public final class LocalOtpVerifyClient implements PhoneVerifyGateway {
    private static final Logger LOG = LoggerFactory.getLogger(LocalOtpVerifyClient.class);
    private static final Pattern LOCAL_JDBC = Pattern.compile(
        "^jdbc:mysql://(?:127\\.0\\.0\\.1|localhost):3307/sajha(?:\\?.*)?$"
    );
    private record Pending(String code, Instant expiresAt) {}
    private final Map<String, Pending> pending = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public LocalOtpVerifyClient(Environment environment) {
        String address = environment.getProperty("server.address", "");
        String jdbcUrl = environment.getProperty("spring.datasource.url", "");
        if (!"127.0.0.1".equals(address) || !LOCAL_JDBC.matcher(jdbcUrl).matches()) {
            throw new IllegalStateException(
                "Local OTP can run only with server.address=127.0.0.1 and " +
                "a localhost MySQL database at 127.0.0.1:3307/sajha. " +
                "Never use this provider with Render/Aiven or a public server."
            );
        }
        for (String profile : environment.getActiveProfiles()) {
            if (!"local".equals(profile)) {
                throw new IllegalStateException("Local OTP requires only the 'local' Spring profile");
            }
        }
        LOG.warn("SaJHa LOCAL TEST OTP provider active: no real phone ownership is verified.");
    }

    @Override
    public void assertConfigured() {
        // Constructor guards ensure this provider only runs against the local DB and loopback.
    }

    @Override
    public void send(String phone) {
        String code = "%06d".formatted(random.nextInt(1_000_000));
        pending.put(phone, new Pending(code, Instant.now().plusSeconds(600)));
        String suffix = phone.substring(Math.max(0, phone.length() - 4));
        LOG.warn("SaJHa LOCAL TEST OTP for phone ending {}: {} (10-minute expiry)", suffix, code);
    }

    @Override
    public boolean check(String phone, String code) {
        Pending entry = pending.get(phone);
        if (entry == null) return false;
        if (!Instant.now().isBefore(entry.expiresAt())) {
            pending.remove(phone, entry);
            return false;
        }
        boolean correct = MessageDigest.isEqual(
            entry.code().getBytes(StandardCharsets.UTF_8),
            code.getBytes(StandardCharsets.UTF_8)
        );
        if (correct) return pending.remove(phone, entry);
        return false;
    }
}
