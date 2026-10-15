<?php

namespace App\Http\Resources;

use Illuminate\Http\Request;

/**
 * docs/openapi.yaml AdminCategory: Category plus productCount — the
 * number of products whose status is not archived. Controllers supply it
 * via withCount(products as productCount) with the archived filter.
 */
class AdminCategoryResource extends CategoryResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        return array_merge(parent::toArray($request), [
            'productCount' => (int) ($this->productCount ?? 0),
        ]);
    }
}
