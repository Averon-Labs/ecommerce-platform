<?php

namespace App\Http\Controllers;

use App\Exceptions\ApiException;
use App\Http\Requests\GoogleAuthRequest;
use App\Models\User;
use App\Services\AuthSessionIssuer;
use App\Services\GoogleAuthService;
use App\Support\GoogleIdentity;
use Illuminate\Database\QueryException;
use Illuminate\Http\JsonResponse;

/**
 * AUTH-004: Google Sign-In / Sign-Up. A verified Google ID token signs an
 * existing customer in, links the Google identity to an existing password
 * account, or creates a password-less customer account. Only the Google
 * account subject (`sub`) is stored; the token itself is never persisted.
 */
class GoogleAuthController extends Controller
{
    public function __construct(
        private readonly GoogleAuthService $google,
        private readonly AuthSessionIssuer $sessions,
    ) {}

    public function login(GoogleAuthRequest $request): JsonResponse
    {
        $identity = $this->google->verify($request->string('idToken')->toString());

        $user = User::where('google_id', $identity->subject)->first()
            ?? User::where('email', $identity->email)->first();

        $created = $user === null;

        if ($created) {
            $user = $this->createCustomer($identity);
            $created = $user->wasRecentlyCreated;
        }

        if ($user->status !== 'active') {
            throw new ApiException(
                401,
                'INVALID_CREDENTIALS',
                'The Google sign-in credential is invalid or expired.',
            );
        }

        if ($user->google_id === null) {
            $user->google_id = $identity->subject;
            $user->save();
        } elseif ($user->google_id !== $identity->subject) {
            throw new ApiException(
                409,
                'GOOGLE_ACCOUNT_CONFLICT',
                'The email is already linked to a different Google account.',
            );
        }

        return $this->sessions->issue($user, $created ? 201 : 200);
    }

    /**
     * Create the account for a first-time Google identity. A concurrent
     * duplicate answers with the existing row so the request still signs in.
     */
    private function createCustomer(GoogleIdentity $identity): User
    {
        try {
            return User::create([
                'first_name' => $identity->firstName,
                'last_name' => $identity->lastName,
                'email' => $identity->email,
                'google_id' => $identity->subject,
                'email_verified_at' => now(),
            ]);
        } catch (QueryException $e) {
            if ((string) $e->getCode() !== '23000') {
                throw $e;
            }

            return User::where('email', $identity->email)
                ->orWhere('google_id', $identity->subject)
                ->first() ?? throw $e;
        }
    }
}
