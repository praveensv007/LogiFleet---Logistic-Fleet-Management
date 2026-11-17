package com.logistics.entity.enums;

public enum RoleName {
    ADMIN,
    FLEET_MANAGER,
    WAREHOUSE_STAFF,
    DRIVER,
    CUSTOMER;

    /** Spring Security authority name stored in roles.name, e.g. ROLE_ADMIN */
    public String authority() {
        return "ROLE_" + name();
    }

}
