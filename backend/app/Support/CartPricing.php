<?php

namespace App\Support;

use App\Models\Cart;
use App\Models\CartItem;
use Illuminate\Support\Collection;

/**
 * ORD-001/CART-001: server-side cart pricing shared by GET /cart,
 * POST /checkout/quote, and POST /orders, so the quote and the created
 * order are guaranteed to match what the cart displayed. Every amount is
 * integer cents (x-money-rules); lines are always re-priced from the
 * current product record.
 */
final class CartPricing
{
    /** Flat sample shipping per x-money-rules (OPEN-3): USD 5.00 per order. */
    public const SAMPLE_SHIPPING_CENTS = 500;

    /**
     * Price a cart at the current product records.
     *
     * @return array{
     *     lines: Collection<int, array{item: CartItem, unitCents: int, lineCents: int, available: int, active: bool, repriced: bool}>,
     *     subtotalCents: int,
     *     itemCount: int,
     *     hasIssues: bool
     * }
     */
    public static function quote(?Cart $cart): array
    {
        $cart?->load(['items.product.inventory', 'items.product.images']);
        $items = $cart?->items ?? collect();

        $lines = $items->map(function (CartItem $item) {
            $product = $item->product;
            $unitCents = Money::toCents((string) $product->price);

            return [
                'item' => $item,
                'unitCents' => $unitCents,
                'lineCents' => $unitCents * $item->quantity,
                'available' => $product->inventory?->quantity ?? 0,
                'active' => $product->status === 'active',
                'repriced' => Money::toCents((string) $item->unit_price) !== $unitCents,
            ];
        });

        return [
            'lines' => $lines,
            'subtotalCents' => (int) $lines->sum('lineCents'),
            'itemCount' => (int) $items->sum('quantity'),
            'hasIssues' => $lines->contains(
                fn (array $line) => ! $line['active']
                    || $line['available'] < $line['item']->quantity
                    || $line['repriced'],
            ),
        ];
    }
}
