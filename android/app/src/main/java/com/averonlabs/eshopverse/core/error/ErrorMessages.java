package com.averonlabs.eshopverse.core.error;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.averonlabs.eshopverse.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps stable API error codes to localized user-visible string resources.
 * Always guarantees a fallback to R.string.error_generic.
 */
public final class ErrorMessages {

    private static final Map<String, Integer> CODE_TO_RESOURCE = new HashMap<>();

    static {
        CODE_TO_RESOURCE.put(ApiErrorCodes.VALIDATION_ERROR, R.string.error_validation);
        CODE_TO_RESOURCE.put(ApiErrorCodes.UNAUTHENTICATED, R.string.error_unauthenticated);
        CODE_TO_RESOURCE.put(ApiErrorCodes.INVALID_CREDENTIALS, R.string.error_invalid_credentials);
        CODE_TO_RESOURCE.put(ApiErrorCodes.ACCOUNT_LOCKED, R.string.error_account_locked);
        CODE_TO_RESOURCE.put(ApiErrorCodes.ACCOUNT_DISABLED, R.string.error_account_disabled);
        CODE_TO_RESOURCE.put(ApiErrorCodes.FORBIDDEN, R.string.error_forbidden);
        CODE_TO_RESOURCE.put(ApiErrorCodes.CSRF_TOKEN_MISMATCH, R.string.error_csrf_token_mismatch);
        CODE_TO_RESOURCE.put(ApiErrorCodes.NOT_FOUND, R.string.error_not_found);
        CODE_TO_RESOURCE.put(ApiErrorCodes.EMAIL_ALREADY_REGISTERED, R.string.error_email_already_registered);
        CODE_TO_RESOURCE.put(ApiErrorCodes.RESET_TOKEN_INVALID, R.string.error_reset_token_invalid);
        CODE_TO_RESOURCE.put(ApiErrorCodes.PRODUCT_UNAVAILABLE, R.string.error_product_unavailable);
        CODE_TO_RESOURCE.put(ApiErrorCodes.INSUFFICIENT_STOCK, R.string.error_insufficient_stock);
        CODE_TO_RESOURCE.put(ApiErrorCodes.CART_ITEM_NOT_FOUND, R.string.error_cart_item_not_found);
        CODE_TO_RESOURCE.put(ApiErrorCodes.CART_EMPTY, R.string.error_cart_empty);
        CODE_TO_RESOURCE.put(ApiErrorCodes.MISSING_IDEMPOTENCY_KEY, R.string.error_missing_idempotency_key);
        CODE_TO_RESOURCE.put(ApiErrorCodes.IDEMPOTENCY_KEY_REUSED, R.string.error_idempotency_key_reused);
        CODE_TO_RESOURCE.put(ApiErrorCodes.INVALID_ORDER_STATE, R.string.error_invalid_order_state);
        CODE_TO_RESOURCE.put(ApiErrorCodes.INVALID_TRANSITION, R.string.error_invalid_transition);
        CODE_TO_RESOURCE.put(ApiErrorCodes.INVALID_PRODUCT_STATE, R.string.error_invalid_product_state);
        CODE_TO_RESOURCE.put(ApiErrorCodes.INVALID_CATEGORY_STATE, R.string.error_invalid_category_state);
        CODE_TO_RESOURCE.put(ApiErrorCodes.CATEGORY_NOT_EMPTY, R.string.error_category_not_empty);
        CODE_TO_RESOURCE.put(ApiErrorCodes.CATEGORY_NAME_TAKEN, R.string.error_category_name_taken);
        CODE_TO_RESOURCE.put(ApiErrorCodes.SKU_TAKEN, R.string.error_sku_taken);
        CODE_TO_RESOURCE.put(ApiErrorCodes.CATEGORY_NOT_FOUND, R.string.error_category_not_found);
        CODE_TO_RESOURCE.put(ApiErrorCodes.RATE_LIMITED, R.string.error_rate_limited);
        CODE_TO_RESOURCE.put(ApiErrorCodes.INTERNAL_ERROR, R.string.error_internal);
    }

    private ErrorMessages() {
    }

    /**
     * Resolves an API error code to a string resource.
     * Returns R.string.error_generic if the code is unknown, null, or unmapped.
     */
    @StringRes
    public static int getMessageRes(@Nullable String code) {
        if (code == null) {
            return R.string.error_generic;
        }
        Integer resId = CODE_TO_RESOURCE.get(code);
        return resId != null ? resId : R.string.error_generic;
    }

    /**
     * Checks if a code has an explicit mapped string resource.
     */
    public static boolean hasExplicitMapping(@NonNull String code) {
        return CODE_TO_RESOURCE.containsKey(code);
    }
}
