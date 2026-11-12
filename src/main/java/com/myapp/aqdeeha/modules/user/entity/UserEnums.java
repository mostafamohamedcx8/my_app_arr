package com.myapp.aqdeeha.modules.user.entity;

import com.myapp.aqdeeha.common.util.LowercaseEnumConverter;

/** Enums stored on the {@code users} table (as lower-case strings). */
public final class UserEnums {

    private UserEnums() {
    }

    public enum UserRole {
        CUSTOMER, COURIER;

        @jakarta.persistence.Converter
        public static class DbConverter extends LowercaseEnumConverter<UserRole> {
            public DbConverter() {
                super(UserRole.class);
            }
        }
    }

    public enum UserStatus {
        ACTIVE, PENDING, SUSPENDED, BANNED;

        @jakarta.persistence.Converter
        public static class DbConverter extends LowercaseEnumConverter<UserStatus> {
            public DbConverter() {
                super(UserStatus.class);
            }
        }
    }

    public enum Gender {
        MALE, FEMALE;

        @jakarta.persistence.Converter
        public static class DbConverter extends LowercaseEnumConverter<Gender> {
            public DbConverter() {
                super(Gender.class);
            }
        }
    }
}
