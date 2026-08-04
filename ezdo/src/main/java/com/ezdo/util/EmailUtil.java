package com.ezdo.util;

/**
 * The single rule for email identity. Email is the join key between the OTP and
 * Google login paths, so both must normalize the same way or a user ends up with
 * two accounts.
 */
public final class EmailUtil {

    private EmailUtil() {}

    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
