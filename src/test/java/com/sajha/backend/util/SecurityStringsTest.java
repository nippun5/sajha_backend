package com.sajha.backend.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SecurityStringsTest {
    @Test void phonesMustBeE164(){
        assertTrue(SecurityStrings.validPhone("+13145550123"));
        assertFalse(SecurityStrings.validPhone(""));
        assertFalse(SecurityStrings.validPhone("314-555-0123"));
        assertFalse(SecurityStrings.validPhone("+012345678"));
    }
    @Test void tokenIsRandomAndHashIsDeterministic(){
        String a=SecurityStrings.randomToken(), b=SecurityStrings.randomToken();
        assertNotEquals(a,b);
        assertNotEquals(SecurityStrings.sha256(a),SecurityStrings.sha256(b));
        assertEquals(64,SecurityStrings.sha256(a).length());
    }
    @Test void emailIsNormalized(){assertEquals("person@example.com",SecurityStrings.normalizeEmail(" Person@Example.COM "));}
}
