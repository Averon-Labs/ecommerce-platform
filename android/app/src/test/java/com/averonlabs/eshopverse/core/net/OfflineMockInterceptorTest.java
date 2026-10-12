package com.averonlabs.eshopverse.core.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OfflineMockInterceptorTest {

    @Test
    public void testOfflineFallbackReturnsSampleJsonOnConnectionFailure() throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new OfflineMockInterceptor())
                .build();

        // Target an unreachable dummy local port to force IOException
        Request request = new Request.Builder()
                .url("http://127.0.0.1:54321/api/v1/categories")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(200, response.code());
            assertNotNull(response.body());
            String body = response.body().string();
            assertTrue(body.contains("Footwear"));
            assertTrue(body.contains("Apparel"));
        }
    }

    @Test
    public void testOfflineProductsFallback() throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new OfflineMockInterceptor())
                .build();

        Request request = new Request.Builder()
                .url("http://127.0.0.1:54321/api/v1/products")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(200, response.code());
            assertNotNull(response.body());
            String body = response.body().string();
            assertTrue(body.contains("Classic Leather Sneaker"));
        }
    }
}
