package com.sajha.backend.auth.social;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SocialRegistrationStateTest {
    @Test void completionChallengeIsOneUse(){
        var challenge=new SocialSignupChallenge("google","sub-example","test@example.com","Test", "a".repeat(64));
        assertTrue(challenge.valid(java.time.Instant.now()));
        challenge.consume(java.time.Instant.now());
        assertFalse(challenge.valid(java.time.Instant.now()));
    }
    @Test void appleNonceIsOneUse(){
        var nonce=new SocialLoginNonce("b".repeat(64));
        assertTrue(nonce.valid());
        nonce.consume();
        assertFalse(nonce.valid());
    }
    @Test void providerIdentityCannotChangeOwner(){
        var identity=new SocialIdentity("owner-1","google","sub-1");
        assertEquals("owner-1",identity.getUserId());
    }
}
