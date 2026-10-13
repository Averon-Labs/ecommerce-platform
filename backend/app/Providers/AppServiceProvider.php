<?php

namespace App\Providers;

use Illuminate\Auth\Notifications\ResetPassword;
use Illuminate\Cache\RateLimiting\Limit;
use Illuminate\Http\Request;
use Illuminate\Notifications\Messages\MailMessage;
use Illuminate\Support\Facades\RateLimiter;
use Illuminate\Support\ServiceProvider;

class AppServiceProvider extends ServiceProvider
{
    /**
     * Register any application services.
     */
    public function register(): void
    {
        //
    }

    /**
     * Bootstrap any application services.
     */
    public function boot(): void
    {
        /*
         * Password-reset mail (docs/openapi.yaml /auth/password-reset): the
         * API has no web reset page, so the mail carries the raw token for
         * the local mail sink instead of a `password.reset` named route.
         */
        ResetPassword::toMailUsing(
            fn ($notifiable, string $token) => (new MailMessage)
                ->subject('Reset your EShop Verse password')
                ->line('You are receiving this email because we received a password reset request for your account.')
                ->line('Your password reset token:')
                ->line($token)
                ->line('This password reset token will expire in '.config('auth.passwords.users.expire').' minutes.')
                ->line('If you did not request a password reset, no further action is required.'),
        );

        /*
         * Rate-limit classes from docs/openapi.yaml x-rate-limit-classes
         * (customer auth: AUTH-001, catalog: CAT-001, admin: AUTH-003).
         */
        RateLimiter::for('auth-login', function (Request $request) {
            return Limit::perMinute(10)->by($request->ip());
        });

        RateLimiter::for('auth-register', function (Request $request) {
            return Limit::perHour(5)->by($request->ip());
        });

        RateLimiter::for('password-reset', function (Request $request) {
            $email = $request->input('email');

            return [
                Limit::perHour(3)->by('password-reset:'.(is_string($email) ? strtolower($email) : 'invalid')),
                Limit::perHour(10)->by('password-reset-ip:'.$request->ip()),
            ];
        });

        RateLimiter::for('public-read', function (Request $request) {
            return Limit::perMinute(120)->by($request->ip());
        });
    }
}
