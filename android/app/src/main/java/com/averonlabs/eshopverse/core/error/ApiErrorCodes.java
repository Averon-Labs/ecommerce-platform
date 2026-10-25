package com.averonlabs.eshopverse.core.error;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Constants for all stable machine-readable error codes defined in the OpenAPI contract.
 */
public final class ApiErrorCodes {

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String UNAUTHENTICATED = "UNAUTHENTICATED";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String ACCOUNT_DISABLED = "ACCOUNT_DISABLED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String CSRF_TOKEN_MISMATCH = "CSRF_TOKEN_MISMATCH";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String EMAIL_ALREADY_REGISTERED = "EMAIL_ALREADY_REGISTERED";
    public static final String RESET_TOKEN_INVALID = "RESET_TOKEN_INVALID";
    public static final String PRODUCT_UNAVAILABLE = "PRODUCT_UNAVAILABLE";
    public static final String INSUFFICIENT_STOCK = "INSUFFICIENT_STOCK";
    public static final String CART_ITEM_NOT_FOUND = "CART_ITEM_NOT_FOUND";
    public static final String CART_EMPTY = "CART_EMPTY";
    public static final String MISSING_IDEMPOTENCY_KEY = "MISSING_IDEMPOTENCY_KEY";
    public static final String IDEMPOTENCY_KEY_REUSED = "IDEMPOTENCY_KEY_REUSED";
    public static final String INVALID_ORDER_STATE = "INVALID_ORDER_STATE";
    public static final String INVALID_TRANSITION = "INVALID_TRANSITION";
    public static final String INVALID_PRODUCT_STATE = "INVALID_PRODUCT_STATE";
    public static final String INVALID_CATEGORY_STATE = "INVALID_CATEGORY_STATE";
    public static final String CATEGORY_NOT_EMPTY = "CATEGORY_NOT_EMPTY";
    public static final String CATEGORY_NAME_TAKEN = "CATEGORY_NAME_TAKEN";
    public static final String SKU_TAKEN = "SKU_TAKEN";
    public static final String CATEGORY_NOT_FOUND = "CATEGORY_NOT_FOUND";
    public static final String RATE_LIMITED = "RATE_LIMITED";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    public static final List<String> ALL_CODES = Collections.unmodifiableList(Arrays.asList(
            VALIDATION_ERROR,
            UNAUTHENTICATED,
            INVALID_CREDENTIALS,
            ACCOUNT_LOCKED,
            ACCOUNT_DISABLED,
            FORBIDDEN,
            CSRF_TOKEN_MISMATCH,
            NOT_FOUND,
            EMAIL_ALREADY_REGISTERED,
            RESET_TOKEN_INVALID,
            PRODUCT_UNAVAILABLE,
            INSUFFICIENT_STOCK,
            CART_ITEM_NOT_FOUND,
            CART_EMPTY,
            MISSING_IDEMPOTENCY_KEY,
            IDEMPOTENCY_KEY_REUSED,
            INVALID_ORDER_STATE,
            INVALID_TRANSITION,
            INVALID_PRODUCT_STATE,
            INVALID_CATEGORY_STATE,
            CATEGORY_NOT_EMPTY,
            CATEGORY_NAME_TAKEN,
            SKU_TAKEN,
            CATEGORY_NOT_FOUND,
            RATE_LIMITED,
            INTERNAL_ERROR
    ));

    private ApiErrorCodes() {
    }
}
