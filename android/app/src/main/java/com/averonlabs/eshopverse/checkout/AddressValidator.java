package com.averonlabs.eshopverse.checkout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.core.model.ShippingAddress;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validates shipping address fields according to openapi.yaml specifications
 * and PHASE-2-SPEC.md §E.3.
 */
public final class AddressValidator {

    public static final String FIELD_RECIPIENT_NAME = "recipientName";
    public static final String FIELD_LINE1 = "line1";
    public static final String FIELD_LINE2 = "line2";
    public static final String FIELD_CITY = "city";
    public static final String FIELD_REGION = "region";
    public static final String FIELD_POSTAL_CODE = "postalCode";
    public static final String FIELD_COUNTRY = "country";

    private static final Pattern COUNTRY_PATTERN = Pattern.compile("^[A-Z]{2}$");

    public static final class ValidationResult {
        private final boolean valid;
        private final Map<String, Integer> fieldErrors;

        public ValidationResult(boolean valid, @NonNull Map<String, Integer> fieldErrors) {
            this.valid = valid;
            this.fieldErrors = Collections.unmodifiableMap(new HashMap<>(fieldErrors));
        }

        public boolean isValid() {
            return valid;
        }

        @NonNull
        public Map<String, Integer> getFieldErrors() {
            return fieldErrors;
        }

        @Nullable
        public Integer getFieldError(@NonNull String field) {
            return fieldErrors.get(field);
        }
    }

    private AddressValidator() {
        // Utility class
    }

    /**
     * Normalizes and uppercases a 2-letter country code.
     */
    @NonNull
    public static String normalizeCountry(@Nullable String country) {
        if (country == null) {
            return "";
        }
        return country.trim().toUpperCase();
    }

    /**
     * Validates individual field values and returns a ValidationResult.
     */
    @NonNull
    public static ValidationResult validate(
            @Nullable String recipientName,
            @Nullable String line1,
            @Nullable String line2,
            @Nullable String city,
            @Nullable String region,
            @Nullable String postalCode,
            @Nullable String country
    ) {
        Map<String, Integer> errors = new HashMap<>();

        String cleanRecipient = recipientName != null ? recipientName.trim() : "";
        if (cleanRecipient.isEmpty() || cleanRecipient.length() > 100) {
            errors.put(FIELD_RECIPIENT_NAME, R.string.checkout_error_recipient_name_required);
        }

        String cleanLine1 = line1 != null ? line1.trim() : "";
        if (cleanLine1.isEmpty() || cleanLine1.length() > 150) {
            errors.put(FIELD_LINE1, R.string.checkout_error_line1_required);
        }

        String cleanLine2 = line2 != null ? line2.trim() : "";
        if (cleanLine2.length() > 150) {
            errors.put(FIELD_LINE2, R.string.checkout_error_line2_too_long);
        }

        String cleanCity = city != null ? city.trim() : "";
        if (cleanCity.isEmpty() || cleanCity.length() > 60) {
            errors.put(FIELD_CITY, R.string.checkout_error_city_required);
        }

        String cleanRegion = region != null ? region.trim() : "";
        if (cleanRegion.isEmpty() || cleanRegion.length() > 60) {
            errors.put(FIELD_REGION, R.string.checkout_error_region_required);
        }

        String cleanPostalCode = postalCode != null ? postalCode.trim() : "";
        if (cleanPostalCode.isEmpty() || cleanPostalCode.length() > 20) {
            errors.put(FIELD_POSTAL_CODE, R.string.checkout_error_postal_code_required);
        }

        String cleanCountry = normalizeCountry(country);
        if (!COUNTRY_PATTERN.matcher(cleanCountry).matches()) {
            errors.put(FIELD_COUNTRY, R.string.checkout_error_country_invalid);
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Helper to create a validated ShippingAddress if valid, or null otherwise.
     */
    @Nullable
    public static ShippingAddress buildIfValid(
            @Nullable String recipientName,
            @Nullable String line1,
            @Nullable String line2,
            @Nullable String city,
            @Nullable String region,
            @Nullable String postalCode,
            @Nullable String country
    ) {
        ValidationResult result = validate(recipientName, line1, line2, city, region, postalCode, country);
        if (!result.isValid()) {
            return null;
        }

        String cleanLine2 = line2 != null && !line2.trim().isEmpty() ? line2.trim() : null;
        return new ShippingAddress(
                recipientName != null ? recipientName.trim() : "",
                line1 != null ? line1.trim() : "",
                cleanLine2,
                city != null ? city.trim() : "",
                region != null ? region.trim() : "",
                postalCode != null ? postalCode.trim() : "",
                normalizeCountry(country)
        );
    }
}
