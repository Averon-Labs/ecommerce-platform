package com.averonlabs.eshopverse.core.error;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.averonlabs.eshopverse.R;

import org.junit.Test;

public class ErrorMessagesTest {

    @Test
    public void testAllDocumentedCodesHaveExplicitMapping() {
        assertEquals(26, ApiErrorCodes.ALL_CODES.size());

        for (String code : ApiErrorCodes.ALL_CODES) {
            assertTrue("Code should be explicitly mapped: " + code, ErrorMessages.hasExplicitMapping(code));
            int resId = ErrorMessages.getMessageRes(code);
            assertNotEquals("Mapped resource for " + code + " must not be generic fallback",
                    R.string.error_generic, resId);
            assertTrue("Resource ID must be valid", resId > 0);
        }
    }

    @Test
    public void testAccountDisabledMapping() {
        assertEquals(R.string.error_account_disabled, ErrorMessages.getMessageRes(ApiErrorCodes.ACCOUNT_DISABLED));
    }

    @Test
    public void testUnknownAndNullCodeFallbacks() {
        assertEquals(R.string.error_generic, ErrorMessages.getMessageRes(null));
        assertEquals(R.string.error_generic, ErrorMessages.getMessageRes("UNKNOWN_RANDOM_CODE"));
        assertEquals(R.string.error_generic, ErrorMessages.getMessageRes(""));
    }
}
