<?php

namespace App\Http\Middleware;

use App\Exceptions\ApiException;
use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

/**
 * AUTH-003 backend portion: server-side admin role gate for every
 * protected /admin catalog route. Hiding controls in the dashboard UI is
 * never sufficient — authorization is enforced here. The contract answers
 * every non-admin credential with 401 UNAUTHENTICATED ("not a valid
 * administrator session"); 403 FORBIDDEN is reserved for a session whose
 * account was demoted, reported by GET /admin/auth/session.
 */
class EnsureUserIsAdmin
{
    /**
     * @param  Closure(Request): Response  $next
     */
    public function handle(Request $request, Closure $next): Response
    {
        $user = $request->user();

        // Defensive: `auth:sanctum` runs first, so this only guards a
        // future direct use of the alias without the auth middleware.
        if ($user === null) {
            throw new ApiException(401, 'UNAUTHENTICATED', 'Authentication is required for this endpoint.');
        }

        // The contract answers every non-admin credential on an admin
        // route with 401 UNAUTHENTICATED ("not a valid administrator
        // session"): guest, customer account, demoted or suspended
        // administrator alike. 403 FORBIDDEN is reserved for a session
        // whose account was demoted, reported by GET /admin/auth/session.
        if ($user->role !== 'admin' || $user->status !== 'active') {
            throw new ApiException(401, 'UNAUTHENTICATED', 'A valid administrator session is required.');
        }

        return $next($request);
    }
}
