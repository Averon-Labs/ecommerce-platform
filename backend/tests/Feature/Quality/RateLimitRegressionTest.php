<?php

namespace Tests\Feature\Quality;

use Illuminate\Cache\RateLimiting\Limit;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\RateLimiter;
use Tests\TestCase;

/**
 * QUAL-003 (backend contribution): closes the documented rate-limit gap.
 * docs/openapi.yaml x-rate-limit-classes defines a `global` 300/min/ip
 * limiter that applies to every operation in addition to narrower classes,
 * but before this phase only the narrower per-operation limiters existed —
 * the "global-only" operations (getCart, listOrders, getOrder, the admin
 * reads, getCustomerProfile) carried no throttle at all. This test pins the
 * wiring: a global-only endpoint now answers 429 RATE_LIMITED, in the
 * documented envelope, once the (test-lowered) global budget is spent. The
 * limit is re-registered to a tiny value so the proof stays fast.
 */
class RateLimitRegressionTest extends TestCase
{
    use RefreshDatabase;

    public function test_a_global_only_endpoint_enforces_the_global_rate_limiter(): void
    {
        [, $token] = $this->registerCustomer();

        // Re-register after sign-in on a dedicated key so the setup request
        // does not share the tiny budget; the middleware resolves the limiter
        // at request time, so the override applies to the calls below.
        RateLimiter::for('global', fn () => Limit::perMinute(2)->by('quality-global-test'));

        // GET /cart is a documented "global-only" operation (no narrower class).
        $this->withToken($token)->getJson('/api/v1/cart')->assertOk();
        $this->withToken($token)->getJson('/api/v1/cart')->assertOk();

        $limited = $this->withToken($token)->getJson('/api/v1/cart');

        $this->assertApiError($limited, 429, 'RATE_LIMITED');
        $limited->assertHeader('Retry-After');
    }
}
