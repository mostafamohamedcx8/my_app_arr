package com.myapp.aqdeeha.common.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"Passw0rd!", "abc123#x", "كلمة123@سر"})
    void acceptsStrongPasswords(String pw) {
        assertTrue(pw.matches(PasswordPolicy.REGEX));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Pa0!", "password", "12345678", "Password1", "Password!", "12345678!", "pass word1"})
    void rejectsWeakPasswords(String pw) {
        assertFalse(pw.matches(PasswordPolicy.REGEX));
    }
}
