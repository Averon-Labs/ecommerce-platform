package com.averonlabs.eshopverse.cart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.averonlabs.eshopverse.core.model.Cart;
import com.averonlabs.eshopverse.core.model.CartItem;
import com.averonlabs.eshopverse.core.model.Currency;
import com.averonlabs.eshopverse.core.money.Money;
import com.averonlabs.eshopverse.core.money.MoneyFormat;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Proves per docs/android/PHASE-2-SPEC.md §D.4/§D.5 that the UI layer never sums money itself;
 * the values rendered strictly equal the server strings, with no floating-point arithmetic.
 */
public class CartTotalsTest {

    private final Gson gson = new Gson();

    @Test
    public void testServerAuthoritativeTotalsMatchExactStrings() {
        InputStream stream = getClass().getResourceAsStream("/fixtures/api/cart/populated.json");
        assertNotNull("populated.json fixture must be present", stream);

        JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        Cart cart = gson.fromJson(root.getAsJsonObject("data"), Cart.class);

        // Server strings
        assertEquals("204.48", cart.getSubtotal());
        assertEquals("5.00", cart.getShipping());
        assertEquals("209.48", cart.getTotal());

        // Rendered formatted strings match exact server-returned values
        assertEquals("$204.48", MoneyFormat.format(cart.getSubtotalMoney()));
        assertEquals("$5.00", MoneyFormat.format(cart.getShippingMoney()));
        assertEquals("$209.48", MoneyFormat.format(cart.getTotalMoney()));

        // Line item totals also match server strings
        assertEquals(2, cart.getItems().size());
        CartItem item1 = cart.getItems().get(0);
        assertEquals("89.99", item1.getUnitPrice());
        assertEquals("179.98", item1.getLineTotal());
        assertEquals("$89.99", MoneyFormat.format(item1.getUnitPriceMoney()));
        assertEquals("$179.98", MoneyFormat.format(item1.getLineTotalMoney()));
    }

    @Test
    public void testEmptyCartTotalsEqualZeroStrings() {
        InputStream stream = getClass().getResourceAsStream("/fixtures/api/cart/empty.json");
        assertNotNull("empty.json fixture must be present", stream);

        JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        Cart cart = gson.fromJson(root.getAsJsonObject("data"), Cart.class);

        assertEquals("0.00", cart.getSubtotal());
        assertEquals("0.00", cart.getShipping());
        assertEquals("0.00", cart.getTotal());

        assertEquals("$0.00", MoneyFormat.format(cart.getSubtotalMoney()));
        assertEquals("$0.00", MoneyFormat.format(cart.getShippingMoney()));
        assertEquals("$0.00", MoneyFormat.format(cart.getTotalMoney()));
        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    public void testCartClassesDoNotExposeDoubleOrFloatOperations() {
        // Enforce Ponytail and Architecture Rules: No float or double arithmetic exposed for money
        Class<?>[] classes = new Class<?>[]{Cart.class, CartItem.class, CartViewModel.class};
        for (Class<?> clazz : classes) {
            for (Method method : clazz.getDeclaredMethods()) {
                Class<?> returnType = method.getReturnType();
                assertFalse("Method " + method.getName() + " in " + clazz.getSimpleName() + " must not return double",
                        returnType.equals(double.class) || returnType.equals(Double.class));
                assertFalse("Method " + method.getName() + " in " + clazz.getSimpleName() + " must not return float",
                        returnType.equals(float.class) || returnType.equals(Float.class));

                for (Class<?> paramType : method.getParameterTypes()) {
                    assertFalse("Method " + method.getName() + " in " + clazz.getSimpleName() + " must not accept double",
                            paramType.equals(double.class) || paramType.equals(Double.class));
                    assertFalse("Method " + method.getName() + " in " + clazz.getSimpleName() + " must not accept float",
                            paramType.equals(float.class) || paramType.equals(Float.class));
                }
            }
        }
    }
}
