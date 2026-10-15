<?php

namespace App\Support;

/**
 * Fixed-precision money in integer cents (docs/openapi.yaml x-money-rules):
 * amounts are two-fraction-digit decimal strings; arithmetic never touches
 * binary floating point.
 */
final class Money
{
    /** "39.99" -> 3999. */
    public static function toCents(string $amount): int
    {
        [$whole, $fraction] = array_pad(explode('.', $amount, 2), 2, '0');

        return (int) $whole * 100 + (int) substr($fraction.'00', 0, 2);
    }

    /** 3999 -> "39.99". */
    public static function fromCents(int $cents): string
    {
        return sprintf('%d.%02d', intdiv($cents, 100), $cents % 100);
    }
}
