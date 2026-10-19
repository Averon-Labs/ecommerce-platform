package com.averonlabs.eshopverse.core.net;

import androidx.annotation.Nullable;

import com.averonlabs.eshopverse.core.model.AddCartItemRequest;
import com.averonlabs.eshopverse.core.model.AuthSession;
import com.averonlabs.eshopverse.core.model.Cart;
import com.averonlabs.eshopverse.core.model.Category;
import com.averonlabs.eshopverse.core.model.CheckoutQuote;
import com.averonlabs.eshopverse.core.model.CheckoutQuoteRequest;
import com.averonlabs.eshopverse.core.model.CreateOrderRequest;
import com.averonlabs.eshopverse.core.model.GoogleAuthRequest;
import com.averonlabs.eshopverse.core.model.LoginRequest;
import com.averonlabs.eshopverse.core.model.Order;
import com.averonlabs.eshopverse.core.model.OrderSummary;
import com.averonlabs.eshopverse.core.model.PasswordResetConfirmRequest;
import com.averonlabs.eshopverse.core.model.PasswordResetRequest;
import com.averonlabs.eshopverse.core.model.PaymentResult;
import com.averonlabs.eshopverse.core.model.ProductDetail;
import com.averonlabs.eshopverse.core.model.ProductSummary;
import com.averonlabs.eshopverse.core.model.RegisterRequest;
import com.averonlabs.eshopverse.core.model.SimulatedPaymentRequest;
import com.averonlabs.eshopverse.core.model.UpdateCartItemQuantityRequest;
import com.averonlabs.eshopverse.core.model.UpdateProfileRequest;
import com.averonlabs.eshopverse.core.model.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit route definitions for EShop Verse API endpoints across Phases 1 and 2.
 * Directly corresponds to operations defined in docs/openapi.yaml.
 */
public interface ApiRoutes {

    // ----------------------------------------------------------------- Catalog

    @GET("categories")
    Call<ApiResponse<List<Category>>> listCategories();

    @GET("products")
    Call<PagedResponse<ProductSummary>> listProducts(
            @Query("page") @Nullable Integer page,
            @Query("per_page") @Nullable Integer perPage,
            @Query("category_id") @Nullable Long categoryId,
            @Query("search") @Nullable String search,
            @Query("sort") @Nullable String sort
    );

    @GET("products/{productId}")
    Call<ApiResponse<ProductDetail>> getProduct(@Path("productId") long productId);

    // ----------------------------------------------------------- Customer Auth

    @POST("auth/register")
    Call<ApiResponse<AuthSession>> registerCustomer(@Body RegisterRequest request);

    @POST("auth/login")
    Call<ApiResponse<AuthSession>> loginCustomer(@Body LoginRequest request);

    @POST("auth/google")
    Call<ApiResponse<AuthSession>> loginWithGoogle(@Body GoogleAuthRequest request);

    @POST("auth/logout")
    Call<Void> logoutCustomer();

    @GET("auth/profile")
    Call<ApiResponse<User>> getCustomerProfile();

    @PATCH("auth/profile")
    Call<ApiResponse<User>> updateCustomerProfile(@Body UpdateProfileRequest request);

    @POST("auth/password-reset")
    Call<Void> requestPasswordReset(@Body PasswordResetRequest request);

    @POST("auth/password-reset/confirm")
    Call<Void> confirmPasswordReset(@Body PasswordResetConfirmRequest request);

    // -------------------------------------------------------------------- Cart

    @GET("cart")
    Call<ApiResponse<Cart>> getCart();

    @POST("cart/items")
    Call<ApiResponse<Cart>> addItemToCart(@Body AddCartItemRequest request);

    @PATCH("cart/items/{productId}")
    Call<ApiResponse<Cart>> updateCartItemQuantity(
            @Path("productId") long productId,
            @Body UpdateCartItemQuantityRequest request
    );

    @DELETE("cart/items/{productId}")
    Call<ApiResponse<Cart>> removeCartItem(@Path("productId") long productId);

    @DELETE("cart")
    Call<Void> clearCart();

    // ---------------------------------------------------------------- Checkout

    @POST("checkout/quote")
    Call<ApiResponse<CheckoutQuote>> getCheckoutQuote(@Body CheckoutQuoteRequest request);

    // ------------------------------------------------------------------ Orders

    @POST("orders")
    Call<ApiResponse<Order>> createOrder(
            @Header("Idempotency-Key") @Nullable String idempotencyKey,
            @Body CreateOrderRequest request
    );

    @POST("orders")
    Call<ApiResponse<Order>> createOrder(@Body CreateOrderRequest request);

    @GET("orders")
    Call<PagedResponse<OrderSummary>> listOrders(
            @Query("page") @Nullable Integer page,
            @Query("per_page") @Nullable Integer perPage
    );

    @GET("orders/{orderId}")
    Call<ApiResponse<Order>> getOrder(@Path("orderId") long orderId);

    @POST("orders/{orderId}/payment")
    Call<ApiResponse<PaymentResult>> submitSimulatedPayment(
            @Header("Idempotency-Key") @Nullable String idempotencyKey,
            @Path("orderId") long orderId,
            @Body SimulatedPaymentRequest request
    );

    @POST("orders/{orderId}/payment")
    Call<ApiResponse<PaymentResult>> submitSimulatedPayment(
            @Path("orderId") long orderId,
            @Body SimulatedPaymentRequest request
    );
}
