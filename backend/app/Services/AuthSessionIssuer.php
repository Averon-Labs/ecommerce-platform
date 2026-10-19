<?php

namespace App\Services;

use App\Http\Resources\UserResource;
use App\Models\User;
use Illuminate\Http\JsonResponse;

/**
 * Issues the shared AuthSession payload (token, absolute expiry, user
 * resource) for every customer sign-in path: AUTH-001 password sign-in
 * and AUTH-004 Google sign-in.
 */
class AuthSessionIssuer
{
    /**
     * Create the Sanctum bearer token and answer with the AuthSession
     * schema defined in docs/openapi.yaml.
     */
    public function issue(User $user, int $status = 200): JsonResponse
    {
        $expiresAt = now()->addMinutes((int) config('sanctum.expiration'));
        $plainTextToken = $user->createToken('android-app', ['*'], $expiresAt)->plainTextToken;

        return response()->json([
            'data' => [
                'token' => $plainTextToken,
                'expiresAt' => $expiresAt->toIso8601String(),
                'user' => new UserResource($user),
            ],
        ], $status);
    }
}
