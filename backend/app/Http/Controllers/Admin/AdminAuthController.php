<?php

namespace App\Http\Controllers\Admin;

use App\Exceptions\ApiException;
use App\Http\Controllers\AuthController;
use App\Http\Controllers\Controller;
use App\Http\Requests\LoginRequest;
use App\Http\Resources\UserResource;
use App\Models\User;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Http\Response;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\Hash;

/**
 * AUTH-003 backend portion: the first-party admin session lifecycle from
 * docs/openapi.yaml (GET /admin/auth/csrf, POST /admin/auth/login,
 * GET /admin/auth/session, POST /admin/auth/logout). Administrators
 * authenticate through the Sanctum stateful session cookie
 * (`eshop_admin_session`) only — no bearer token exists for the admin SPA,
 * and login is reachable only from a stateful origin where CSRF
 * verification applies.
 */
class AdminAuthController extends Controller
{
    /**
     * Issue the XSRF-TOKEN cookie the SPA must echo in the X-XSRF-TOKEN
     * header. The cookie is queued by the CSRF middleware running in the
     * stateful pipeline, so simply answering the stateful GET is enough.
     */
    public function csrf(): Response
    {
        return response()->noContent();
    }

    /**
     * Start an admin session. Wrong passwords and customer accounts get
     * the identical 401 INVALID_CREDENTIALS so the endpoint never
     * discloses which accounts are administrators.
     */
    public function login(LoginRequest $request): JsonResponse
    {
        // No stateful session context means no CSRF protection context;
        // the contract requires a valid X-XSRF-TOKEN before sign-in.
        if (! $request->hasSession()) {
            throw new ApiException(
                403,
                'CSRF_TOKEN_MISMATCH',
                'The X-XSRF-TOKEN header is missing or does not match the cookie.',
            );
        }

        $email = strtolower($request->string('email')->toString());
        $password = $request->string('password')->toString();

        $user = User::where('email', $email)->first();
        $passwordMatches = Hash::check($password, $user?->password ?? AuthController::TIMING_DUMMY_HASH);

        if ($user === null
            || ! $passwordMatches
            || $user->role !== 'admin'
            || $user->status !== 'active') {
            throw new ApiException(401, 'INVALID_CREDENTIALS', 'The email or password is incorrect.');
        }

        Auth::guard('web')->login($user);
        // Session fixation defense, mirroring the framework's login flow.
        $request->session()->regenerate();

        return response()->json([
            'data' => ['user' => new UserResource($user)],
        ]);
    }

    /**
     * Read the current admin session (dashboard bootstrap). A session
     * that outlived its administrator role answers 403 FORBIDDEN; any
     * other non-admin credential is "not a valid administrator session"
     * (401).
     */
    public function session(Request $request): JsonResponse
    {
        $user = $request->user();

        if ($user === null) {
            throw new ApiException(401, 'UNAUTHENTICATED', 'Authentication is required for this endpoint.');
        }

        if ($user->role !== 'admin' || $user->status !== 'active') {
            // Contract Forbidden: "a session exists for an account that is
            // no longer an administrator" — only the session-authenticated
            // path qualifies; a foreign bearer token is a 401.
            if ($request->hasSession() && Auth::guard('web')->check()) {
                throw new ApiException(403, 'FORBIDDEN', 'This account is no longer an administrator.');
            }

            throw new ApiException(401, 'UNAUTHENTICATED', 'A valid administrator session is required.');
        }

        return response()->json([
            'data' => ['user' => new UserResource($user)],
        ]);
    }

    /**
     * End the admin session: log out of the web guard, destroy the
     * session data (invalidate), and rotate the CSRF token. Ending a
     * session requires a session — a bare bearer credential answers 401
     * ("not a valid administrator session").
     */
    public function logout(Request $request): Response
    {
        if ($request->user() === null || ! $request->hasSession()) {
            throw new ApiException(401, 'UNAUTHENTICATED', 'A valid administrator session is required.');
        }

        Auth::guard('web')->logout();
        $request->session()->invalidate();
        $request->session()->regenerateToken();

        return response()->noContent();
    }
}
