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

