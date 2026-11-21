package com.sajha.backend.auth;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthDtosTest {
    @Test void registrationRequiresContactAndStrongPassword(){
        try(var factory=Validation.buildDefaultValidatorFactory()){
            var v=factory.getValidator();
            assertFalse(v.validate(new AuthDtos.Register("A","invalid","123","5551234")).isEmpty());
            assertTrue(v.validate(new AuthDtos.Register("Jane Doe","jane@example.com","passphrase-1234","+13145550123")).isEmpty());
        }
    }
}
