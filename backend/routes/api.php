<?php

/*
|--------------------------------------------------------------------------
| API v1 Routes
|--------------------------------------------------------------------------
|
| The API is mounted under the /api/v1 prefix (see ARCHITECTURE.md) and is
| the only integration boundary for the Android app and the admin dashboard.
|
| Every route below implements an operation from the accepted contract in
| docs/openapi.yaml; responses follow its schemas and error envelope.
|
*/

use App\Http\Controllers\Admin\AdminAuthController;
use App\Http\Controllers\Admin\AdminCategoryController;
use App\Http\Controllers\Admin\AdminInventoryController;
use App\Http\Controllers\Admin\AdminOrderController;
use App\Http\Controllers\Admin\AdminProductController;
use App\Http\Controllers\AuthController;
use App\Http\Controllers\CartController;
use App\Http\Controllers\CategoryController;
use App\Http\Controllers\OrderController;
use App\Http\Controllers\PasswordResetController;
use App\Http\Controllers\ProductController;
use Illuminate\Support\Facades\Route;

// --------------------------------------------------------------- Catalog (public)

Route::get('categories', [CategoryController::class, 'index'])
    ->middleware('throttle:public-read');
Route::get('products', [ProductController::class, 'index'])
    ->middleware('throttle:public-read');
Route::get('products/{productId}', [ProductController::class, 'show'])
    ->whereNumber('productId')
    ->middleware('throttle:public-read');

// ------------------------------------------------------------- Customer auth

Route::prefix('auth')->group(function (): void {
    Route::post('register', [AuthController::class, 'register'])
        ->middleware('throttle:auth-register');
    Route::post('login', [AuthController::class, 'login'])
        ->middleware('throttle:auth-login');
    Route::post('password-reset', [PasswordResetController::class, 'store'])
        ->middleware('throttle:password-reset');
    Route::post('password-reset/confirm', [PasswordResetController::class, 'confirm']);

    Route::middleware('auth:sanctum')->group(function (): void {
        Route::post('logout', [AuthController::class, 'logout']);
        Route::get('profile', [AuthController::class, 'showProfile']);
        Route::patch('profile', [AuthController::class, 'updateProfile']);
    });
});

// ----------------------------------------------------------- Customer cart (CART-001)

Route::middleware('auth:sanctum')->group(function (): void {
    Route::get('cart', [CartController::class, 'show']);
    Route::post('cart/items', [CartController::class, 'store'])
        ->middleware('throttle:customer-write');
    Route::patch('cart/items/{productId}', [CartController::class, 'update'])
        ->whereNumber('productId')
        ->middleware('throttle:customer-write');
    Route::delete('cart/items/{productId}', [CartController::class, 'destroy'])
        ->whereNumber('productId')
        ->middleware('throttle:customer-write');
    Route::delete('cart', [CartController::class, 'clear'])
        ->middleware('throttle:customer-write');
});

// ------------------------------------------------ Checkout and orders (ORD-001)

Route::middleware('auth:sanctum')->group(function (): void {
    Route::post('checkout/quote', [OrderController::class, 'quote'])
        ->middleware('throttle:customer-write');
    Route::post('orders', [OrderController::class, 'store'])
        ->middleware('throttle:customer-write');
    Route::get('orders', [OrderController::class, 'index']);
    Route::get('orders/{orderId}', [OrderController::class, 'show'])
        ->whereNumber('orderId');
    Route::post('orders/{orderId}/payment', [OrderController::class, 'pay'])
        ->whereNumber('orderId')
        ->middleware('throttle:customer-write');
});

// ----------------------------------------------------------- Admin session (AUTH-003)

Route::prefix('admin')->group(function (): void {
    Route::get('auth/csrf', [AdminAuthController::class, 'csrf']);
    Route::post('auth/login', [AdminAuthController::class, 'login'])
        ->middleware('throttle:admin-login');

    Route::middleware('auth:sanctum')->group(function (): void {
        Route::get('auth/session', [AdminAuthController::class, 'session']);
        Route::post('auth/logout', [AdminAuthController::class, 'logout']);

        // ------------------------------------------------------ Admin catalog (CAT-003)

        Route::middleware('admin')->group(function (): void {
            Route::get('products', [AdminProductController::class, 'index']);
            Route::post('products', [AdminProductController::class, 'store'])
                ->middleware('throttle:admin-write');
            Route::get('products/{productId}', [AdminProductController::class, 'show'])
                ->whereNumber('productId');
            Route::put('products/{productId}', [AdminProductController::class, 'update'])
                ->whereNumber('productId')
                ->middleware('throttle:admin-write');
            Route::post('products/{productId}/archive', [AdminProductController::class, 'archive'])
                ->whereNumber('productId')
                ->middleware('throttle:admin-write');

            Route::get('categories', [AdminCategoryController::class, 'index']);
            Route::post('categories', [AdminCategoryController::class, 'store'])
                ->middleware('throttle:admin-write');
            Route::put('categories/{categoryId}', [AdminCategoryController::class, 'update'])
                ->whereNumber('categoryId')
                ->middleware('throttle:admin-write');
            Route::post('categories/{categoryId}/archive', [AdminCategoryController::class, 'archive'])
                ->whereNumber('categoryId')
                ->middleware('throttle:admin-write');
            Route::post('categories/{categoryId}/restore', [AdminCategoryController::class, 'restore'])
                ->whereNumber('categoryId')
                ->middleware('throttle:admin-write');

            Route::get('inventory', [AdminInventoryController::class, 'index']);
            Route::patch('inventory/{productId}', [AdminInventoryController::class, 'update'])
                ->whereNumber('productId')
                ->middleware('throttle:admin-write');

            // ---------------------------------------------------- Admin orders (ORD-001)

            Route::get('orders', [AdminOrderController::class, 'index']);
            Route::get('orders/{orderId}', [AdminOrderController::class, 'show'])
                ->whereNumber('orderId');
            Route::patch('orders/{orderId}/status', [AdminOrderController::class, 'updateStatus'])
                ->whereNumber('orderId')
                ->middleware('throttle:admin-write');
        });
    });
});
