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
use App\Http\Controllers\AuthController;
use App\Http\Controllers\PasswordResetController;
use Illuminate\Support\Facades\Route;

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
    });
});
