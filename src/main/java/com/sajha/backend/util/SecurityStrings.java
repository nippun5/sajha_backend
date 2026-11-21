package com.sajha.backend.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SecurityStrings {
    private static final Pattern E164=Pattern.compile("^\\+[1-9][0-9]{6,14}$");
    private static final SecureRandom RANDOM=new SecureRandom();
    private SecurityStrings() {}
    public static String normalizeEmail(String email){return email.trim().toLowerCase(Locale.ROOT);}
    public static boolean validPhone(String phone){return phone != null && E164.matcher(phone).matches();}
    public static String randomToken(){byte[] bytes=new byte[32];RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    public static String sha256(String value){
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
}
