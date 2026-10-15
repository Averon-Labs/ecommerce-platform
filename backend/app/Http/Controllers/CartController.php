<?php

namespace App\Http\Controllers;

use App\Exceptions\ApiException;
use App\Http\Requests\CartItemStoreRequest;
use App\Http\Requests\CartItemUpdateRequest;
use App\Http\Resources\CartItemResource;
use App\Models\Cart;
use App\Models\Product;
use App\Support\CartPricing;
use App\Support\Money;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/**
 * docs/openapi.yaml Cart operations (getCart, addCartItem, updateCartItem,
 * removeCartItem, clearCart): bearer customers only. The cart is implicit
 * per token — there is no client-facing cart identifier to leak — and every
 * response returns the whole repriced cart so clients never re-read it
 * after a mutation. All money math happens in integer cents.
 */
class CartController extends Controller
{
    /** Hard ceiling of one cart line (CartQuantity.maximum). */
    private const MAX_LINE_QUANTITY = 99;

    /**
     * Read the signed-in customer's cart (empty-cart shape when none exists).
     */
    public function show(Request $request): JsonResponse
    {
        return $this->cartResponse($request->user()->cart);
    }

    /**
     * Add a product; an existing line has its quantity increased.
     */
    public function store(CartItemStoreRequest $request): JsonResponse
    {
        $data = $request->validated();
        $product = $this->availableProduct($data['productId']);
        $user = $request->user();
        $cart = $user->cart ?? $user->cart()->create([]);
        $line = $cart->items()->where('product_id', $product->id)->first();
        $quantity = ($line?->quantity ?? 0) + $data['quantity'];

        if ($quantity > self::MAX_LINE_QUANTITY) {
            throw new ApiException(422, 'VALIDATION_ERROR', 'One or more request fields are invalid.', [
                ['field' => 'quantity', 'message' => 'The resulting cart quantity must not exceed '.self::MAX_LINE_QUANTITY.'.'],
            ]);
        }

        $this->assertStockAvailable($product, $quantity);

        if ($line === null) {
            $cart->items()->create([
                'product_id' => $product->id,
                'quantity' => $quantity,
                'unit_price' => $product->price,
                'added_at' => now(),
            ]);
        } else {
            $line->update(['quantity' => $quantity]);
        }

        return $this->cartResponse($cart);
    }

    /**
     * Set the quantity of one existing line.
     */
    public function update(CartItemUpdateRequest $request, int $productId): JsonResponse
    {
        $cart = $request->user()->cart;
        $line = $cart?->items()->where('product_id', $productId)->first();

        if ($line === null) {
            throw new ApiException(404, 'CART_ITEM_NOT_FOUND', 'The product is not in your cart.');
        }

        $data = $request->validated();
        $product = $line->product;

        if ($product->status !== 'active') {
            throw new ApiException(409, 'PRODUCT_UNAVAILABLE', 'This product can no longer be kept in the cart.');
        }

        $this->assertStockAvailable($product, $data['quantity']);
        $line->update(['quantity' => $data['quantity']]);

        return $this->cartResponse($cart);
    }

    /**
     * Remove one line.
     */
    public function destroy(Request $request, int $productId): JsonResponse
    {
        $cart = $request->user()->cart;
        $line = $cart?->items()->where('product_id', $productId)->first();

        if ($line === null) {
            throw new ApiException(404, 'CART_ITEM_NOT_FOUND', 'The product is not in your cart.');
        }

        $line->delete();

        return $this->cartResponse($cart);
    }

    /**
     * Remove every line (the cart row itself may stay or not exist yet).
     */
    public function clear(Request $request): JsonResponse
    {
        $cart = $request->user()->cart;
        $cart?->items()->delete();

        return $this->cartResponse($cart);
    }

    /**
     * Load a product for a cart mutation: missing -> 404, non-active -> 409.
     */
    private function availableProduct(int $productId): Product
    {
        $product = Product::query()->with('inventory')->find($productId);

        if ($product === null) {
            throw new ApiException(404, 'NOT_FOUND', 'The requested product was not found.');
        }

        if ($product->status !== 'active') {
            throw new ApiException(409, 'PRODUCT_UNAVAILABLE', 'This product can no longer be added to the cart.');
        }

        return $product;
    }

    /**
     * A line may never exceed the product's current available stock.
     */
    private function assertStockAvailable(Product $product, int $quantity): void
    {
        $available = $product->inventory?->quantity ?? 0;

        if ($quantity > $available) {
            throw new ApiException(409, 'INSUFFICIENT_STOCK', 'The requested quantity exceeds the available stock.', [
                ['field' => 'quantity', 'message' => 'Only '.$available.' unit(s) are available.'],
            ]);
        }
    }

    /**
     * Whole-cart response: server-priced lines plus itemCount, subtotal,
     * shipping, total, and the aggregate hasIssues flag. Pricing is shared
     * with the checkout quote (CartPricing), and an empty cart previews no
     * order, so shipping and total are 0.00 there.
     */
    private function cartResponse(?Cart $cart): JsonResponse
    {
        $priced = CartPricing::quote($cart);
        $shippingCents = $priced['lines']->isEmpty() ? 0 : CartPricing::SAMPLE_SHIPPING_CENTS;

        return response()->json([
            'data' => [
                'items' => CartItemResource::collection($priced['lines']->pluck('item'))->resolve(),
                'itemCount' => $priced['itemCount'],
                'subtotal' => Money::fromCents($priced['subtotalCents']),
                'shipping' => Money::fromCents($shippingCents),
                'total' => Money::fromCents($priced['subtotalCents'] + $shippingCents),
                'currency' => 'USD',
                'hasIssues' => $priced['hasIssues'],
            ],
        ]);
    }
}
