package com.logistics.dto.request;

/** Regexes shared by DTO validation (kept identical to src/utils/validators.js in React). */
public final class ValidationPatterns {

    private ValidationPatterns() {
    }

    public static final String NAME = "^[A-Za-z][A-Za-z .]{2,99}$";
    public static final String USERNAME = "^[A-Za-z0-9._]{4,30}$";
    public static final String PHONE = "^[0-9]{10}$";
    public static final String PHONE_OPTIONAL = "^$|^[0-9]{10}$";
    public static final String PINCODE = "^[0-9]{6}$";
    /** Min 8, 1 upper, 1 lower, 1 digit, 1 special */
    public static final String STRONG_PASSWORD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,64}$";
    public static final String GSTIN = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$";
    /** Indian registration number, e.g. TN09AB1234 */
    public static final String VEHICLE_NUMBER = "^[A-Z]{2}[0-9]{1,2}[A-Z]{0,3}[0-9]{4}$";

    public static final String PASSWORD_MESSAGE =
            "Password needs min 8 characters with 1 upper, 1 lower, 1 digit and 1 special character";
}
