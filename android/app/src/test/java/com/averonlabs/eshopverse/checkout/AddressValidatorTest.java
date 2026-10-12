package com.averonlabs.eshopverse.checkout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.core.model.ShippingAddress;

import org.junit.Test;

public class AddressValidatorTest {

    @Test
    public void testValidAddressPasses() {
        AddressValidator.ValidationResult result = AddressValidator.validate(
                "John Doe",
                "123 Main St",
                "Apt 4B",
                "Metropolis",
                "NY",
                "10001",
                "US"
        );

        assertTrue(result.isValid());
        assertTrue(result.getFieldErrors().isEmpty());

        ShippingAddress address = AddressValidator.buildIfValid(
                "John Doe",
                "123 Main St",
                "Apt 4B",
                "Metropolis",
                "NY",
                "10001",
                "US"
        );
        assertNotNull(address);
        assertEquals("John Doe", address.getRecipientName());
        assertEquals("123 Main St", address.getLine1());
        assertEquals("Apt 4B", address.getLine2());
        assertEquals("Metropolis", address.getCity());
        assertEquals("NY", address.getRegion());
        assertEquals("10001", address.getPostalCode());
        assertEquals("US", address.getCountry());
    }

    @Test
    public void testValidAddressWithOptionalLine2Null() {
        AddressValidator.ValidationResult result = AddressValidator.validate(
                "John Doe",
                "123 Main St",
                null,
                "Metropolis",
                "NY",
                "10001",
                "us"
        );

        assertTrue(result.isValid());
        ShippingAddress address = AddressValidator.buildIfValid(
                "John Doe",
                "123 Main St",
                null,
                "Metropolis",
                "NY",
                "10001",
                "us"
        );
        assertNotNull(address);
        assertNull(address.getLine2());
        assertEquals("US", address.getCountry()); // Normalized to uppercase
    }

    @Test
    public void testRequiredFieldsCannotBeEmpty() {
        AddressValidator.ValidationResult result = AddressValidator.validate(
                "", "", null, "", "", "", ""
        );

        assertFalse(result.isValid());
        assertEquals(Integer.valueOf(R.string.checkout_error_recipient_name_required),
                result.getFieldError(AddressValidator.FIELD_RECIPIENT_NAME));
        assertEquals(Integer.valueOf(R.string.checkout_error_line1_required),
                result.getFieldError(AddressValidator.FIELD_LINE1));
        assertEquals(Integer.valueOf(R.string.checkout_error_city_required),
                result.getFieldError(AddressValidator.FIELD_CITY));
        assertEquals(Integer.valueOf(R.string.checkout_error_region_required),
                result.getFieldError(AddressValidator.FIELD_REGION));
        assertEquals(Integer.valueOf(R.string.checkout_error_postal_code_required),
                result.getFieldError(AddressValidator.FIELD_POSTAL_CODE));
        assertEquals(Integer.valueOf(R.string.checkout_error_country_invalid),
                result.getFieldError(AddressValidator.FIELD_COUNTRY));
    }

    @Test
    public void testFieldLengthLimits() {
        String longText101 = new String(new char[101]).replace('\0', 'a');
        String longText151 = new String(new char[151]).replace('\0', 'b');
        String longText61 = new String(new char[61]).replace('\0', 'c');
        String longText21 = new String(new char[21]).replace('\0', 'd');

        AddressValidator.ValidationResult result = AddressValidator.validate(
                longText101,
                longText151,
                longText151,
                longText61,
                longText61,
                longText21,
                "USA" // 3 chars invalid
        );

        assertFalse(result.isValid());
        assertEquals(Integer.valueOf(R.string.checkout_error_recipient_name_required),
                result.getFieldError(AddressValidator.FIELD_RECIPIENT_NAME));
        assertEquals(Integer.valueOf(R.string.checkout_error_line1_required),
                result.getFieldError(AddressValidator.FIELD_LINE1));
        assertEquals(Integer.valueOf(R.string.checkout_error_line2_too_long),
                result.getFieldError(AddressValidator.FIELD_LINE2));
        assertEquals(Integer.valueOf(R.string.checkout_error_city_required),
                result.getFieldError(AddressValidator.FIELD_CITY));
        assertEquals(Integer.valueOf(R.string.checkout_error_region_required),
                result.getFieldError(AddressValidator.FIELD_REGION));
        assertEquals(Integer.valueOf(R.string.checkout_error_postal_code_required),
                result.getFieldError(AddressValidator.FIELD_POSTAL_CODE));
        assertEquals(Integer.valueOf(R.string.checkout_error_country_invalid),
                result.getFieldError(AddressValidator.FIELD_COUNTRY));
    }

    @Test
    public void testCountryCodePattern() {
        assertFalse(AddressValidator.validate("A", "B", null, "C", "D", "E", "12").isValid());
        assertFalse(AddressValidator.validate("A", "B", null, "C", "D", "E", "U1").isValid());
        assertFalse(AddressValidator.validate("A", "B", null, "C", "D", "E", "USA").isValid());
        assertTrue(AddressValidator.validate("A", "B", null, "C", "D", "E", "CA").isValid());
        assertTrue(AddressValidator.validate("A", "B", null, "C", "D", "E", "gb").isValid()); // lower case normalized
    }
}
