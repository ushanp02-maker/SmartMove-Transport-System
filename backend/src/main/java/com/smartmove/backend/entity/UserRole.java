
package com.smartmove.backend.entity;

public enum UserRole {

    PASSENGER,
    DRIVER,
    ADMIN,
    SUPER_ADMIN;

    public boolean isAdmin() {
        return this == ADMIN || this == SUPER_ADMIN;
    }

    public boolean canManageUsers() {
        return this == SUPER_ADMIN;
    }

    public boolean canManageTransport() {
        return this == ADMIN || this == SUPER_ADMIN;
    }
}
