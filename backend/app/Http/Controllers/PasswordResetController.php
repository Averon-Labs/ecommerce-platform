<?php

namespace App\Http\Controllers;

use App\Exceptions\ApiException;
use App\Http\Requests\ConfirmPasswordResetRequest;
use App\Http\Requests\RequestPasswordResetRequest;
use App\Models\User;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Response;
use Illuminate\Support\Facades\Password;

/**
 * docs/openapi.yaml password-reset flow: the request endpoint always
 * answers 202 (no account enumeration), and a successful confirmation
 * revokes every existing bearer token for the account.
 */
class PasswordResetController extends Controller
{
    public function store(RequestPasswordResetRequest $request): JsonResponse
    {
        $email = $request->string('email')->toString();

        // Only deliver a reset link to an existing account; the response
        // is identical either way. Match case-insensitively for legacy rows
        // and use the stored email so the token is keyed consistently.
        $user = User::whereRaw('LOWER(email) = ?', [$email])->first();

        if ($user !== null) {
            Password::broker()->sendResetLink(['email' => $user->email]);
        }

        return response()->json([], 202);
    }

    public function confirm(ConfirmPasswordResetRequest $request): Response
    {
        $email = $request->string('email')->toString();
        $storedEmail = User::whereRaw('LOWER(email) = ?', [$email])->value('email') ?? $email;

        $status = Password::broker()->reset(
            [
                'email' => $storedEmail,
                'token' => $request->string('token')->toString(),
                'password' => $request->string('password')->toString(),
            ],
            function (User $user, string $password): void {
                $user->password = $password;
                $user->save();

                // A successful reset revokes every existing bearer token.
                $user->tokens()->delete();
            },
        );

        if ($status !== Password::PASSWORD_RESET) {
            throw new ApiException(422, 'RESET_TOKEN_INVALID', 'The reset token is invalid or has expired.');
        }

        return response()->noContent();
    }
}
