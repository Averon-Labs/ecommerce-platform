<?php

namespace App\Support;

/**
 * ORD-001: the transitions the admin status endpoint accepts, taken from the
 * docs/openapi.yaml adminUpdateOrderStatus table. `pending_payment` permits
 * only `cancelled`: `confirmed` is reached exclusively through a successful
 * simulated payment (OrderController::pay), never through a recorded
 * transition, so it is not a target here even though x-order-state-machine
 * lists it as the payment's effect. The same machine powers the contract's
 * `allowedTransitions`, so the admin UI never offers a rejected move.
 */
final class OrderStateMachine
{
    /**
     * @var array<string, list<string>>
     */
    private const TRANSITIONS = [
        'pending_payment' => ['cancelled'],
        'confirmed' => ['processing', 'cancelled'],
        'processing' => ['shipped', 'cancelled'],
        'shipped' => ['delivered'],
        'delivered' => [],
        'cancelled' => [],
    ];

    /**
     * @return list<string>
     */
    public static function allowed(string $status): array
    {
        return self::TRANSITIONS[$status] ?? [];
    }

    public static function canTransition(string $from, string $to): bool
    {
        return in_array($to, self::allowed($from), true);
    }
}
