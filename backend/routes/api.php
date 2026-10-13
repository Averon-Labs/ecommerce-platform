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
use App\Http\Controllers\Admin\AdminProductController;
use App\Http\Controllers\AuthController;
use App\Http\Controllers\CategoryController;
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
        });
    });
});
