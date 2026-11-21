package com.sajha.backend.phone;

/** Provider contract for verifying that a user controls a phone number. */
public interface PhoneVerifyGateway {
    void assertConfigured();
    void send(String phone);
    boolean check(String phone, String code);
}
