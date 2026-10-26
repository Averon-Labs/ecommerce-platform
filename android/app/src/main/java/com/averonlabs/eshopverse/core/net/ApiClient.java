package com.averonlabs.eshopverse.core.net;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.averonlabs.eshopverse.BuildConfig;
import com.averonlabs.eshopverse.core.storage.TokenStore;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Builds and configures the unified {@link OkHttpClient} and {@link Retrofit} instances
 * for EShop Verse API communication.
 */
public final class ApiClient {

    private static final long CONNECT_TIMEOUT_SECONDS = 10;
    private static final long READ_TIMEOUT_SECONDS = 20;
    private static final long CALL_TIMEOUT_SECONDS = 30;

    private final OkHttpClient okHttpClient;
    private final Retrofit retrofit;
    private final ApiRoutes routes;

    public ApiClient(@NonNull TokenStore tokenStore, @Nullable UnauthenticatedHandler unauthenticatedHandler) {
        this(BuildConfig.API_BASE_URL, tokenStore, unauthenticatedHandler, BuildConfig.DEBUG);
    }

    public ApiClient(
            @NonNull String baseUrl,
            @NonNull TokenStore tokenStore,
            @Nullable UnauthenticatedHandler unauthenticatedHandler,
            boolean isDebug
    ) {
        Objects.requireNonNull(baseUrl, "baseUrl cannot be null");
        Objects.requireNonNull(tokenStore, "tokenStore cannot be null");

        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .addInterceptor(new AuthInterceptor(tokenStore))
                .addInterceptor(new IdempotencyInterceptor())
                .addInterceptor(new UnauthenticatedInterceptor(unauthenticatedHandler))
                .addInterceptor(new RetryInterceptor());

        if (isDebug) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
            loggingInterceptor.redactHeader("Authorization");
            clientBuilder.addInterceptor(loggingInterceptor);
        }

        this.okHttpClient = clientBuilder.build();

        Gson gson = new GsonBuilder()
                .create();

        this.retrofit = new Retrofit.Builder()
                .baseUrl(normalizedBaseUrl)
                .client(this.okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        this.routes = this.retrofit.create(ApiRoutes.class);
    }

    @NonNull
    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    @NonNull
    public Retrofit getRetrofit() {
        return retrofit;
    }

    @NonNull
    public ApiRoutes getRoutes() {
        return routes;
    }
}
