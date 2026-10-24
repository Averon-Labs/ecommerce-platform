<?php

namespace App\Http\Controllers;

use App\Exceptions\ApiException;
use App\Http\Requests\LoginRequest;
use App\Http\Requests\RegisterRequest;
use App\Http\Requests\UpdateProfileRequest;
use App\Http\Resources\UserResource;
use App\Models\User;
use App\Services\AccountLockout;
use App\Services\AuthSessionIssuer;
use App\Support\ApiError;
use Illuminate\Database\QueryException;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Http\Response;
use Illuminate\Support\Facades\Hash;

/**
 * Customer authentication per docs/openapi.yaml: register, login, logout,
 * and profile read/update, backed by revocable Sanctum bearer tokens.
 */
class AuthController extends Controller
{
    /**
     * The framework's documented example bcrypt hash, used to equalize
     * response timing when the email is unknown (never a real password).
     * Shared with the admin sign-in so both endpoints stay non-specific.
     */
    public const TIMING_DUMMY_HASH = '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi';

    public function __construct(
        private readonly AccountLockout $lockout,
        private readonly AuthSessionIssuer $sessions,
    ) {}

    public function register(RegisterRequest $request): JsonResponse
    {
        $data = $request->validated();

        // The contract answers a taken email with 409 EMAIL_ALREADY_REGISTERED.
        // Compare case-insensitively so a legacy row with different casing
        // cannot be duplicated; new rows are stored lowercase by the model.
        if (User::whereRaw('LOWER(email) = ?', [$data['email']])->exists()) {
            throw new ApiException(409, 'EMAIL_ALREADY_REGISTERED', 'The email is already registered.');
        }

        try {
            $user = User::create([
                'first_name' => $data['firstName'],
                'last_name' => $data['lastName'],
                'email' => $data['email'],
                'password' => $data['password'],
            ]);
        } catch (QueryException $e) {
            // Unique-email race behind the existence check above.
            if ((string) $e->getCode() === '23000') {
                throw new ApiException(409, 'EMAIL_ALREADY_REGISTERED', 'The email is already registered.');
            }

            throw $e;
        }

        return $this->sessions->issue($user, 201);
    }

    public function login(LoginRequest $request): JsonResponse
    {
        $email = strtolower($request->string('email')->toString());
        $password = $request->string('password')->toString();

        if ($this->lockout->isLocked($email)) {
            return $this->accountLockedResponse($email);
        }

        $user = User::where('email', $email)->first();
        $passwordMatches = Hash::check($password, $user?->password ?? self::TIMING_DUMMY_HASH);

        if ($user === null || ! $passwordMatches || $user->status !== 'active') {
            $this->lockout->registerFailure($email);

            throw new ApiException(401, 'INVALID_CREDENTIALS', 'The email or password is incorrect.');
        }

        $this->lockout->clear($email);

        return $this->sessions->issue($user);
    }

    public function logout(Request $request): Response
    {
        $request->user()?->currentAccessToken()?->delete();

        return response()->noContent();
    }

    public function showProfile(Request $request): UserResource
    {
        return new UserResource($request->user());
    }

    public function updateProfile(UpdateProfileRequest $request): UserResource
    {
        $data = $request->validated();
        $user = $request->user();

        foreach (['firstName' => 'first_name', 'lastName' => 'last_name'] as $requestField => $column) {
            if (array_key_exists($requestField, $data)) {
                $user->{$column} = $data[$requestField];
            }
        }

        $user->save();

        return new UserResource($user);
    }

    private function accountLockedResponse(string $email): JsonResponse
    {
        return ApiError::response(
            429,
            'ACCOUNT_LOCKED',
            'Too many failed sign-in attempts. Try again later.',
            [],
            ['Retry-After' => (string) max(1, $this->lockout->retryAfter($email))],
        );
    }
}
