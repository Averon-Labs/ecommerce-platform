<?php

namespace Database\Seeders;

use App\Models\Category;
use App\Models\Inventory;
use App\Models\Product;
use App\Models\ProductImage;
use App\Models\User;
use Illuminate\Database\Console\Seeds\WithoutModelEvents;
use Illuminate\Database\Seeder;

/**
 * CAT-001: reproducible fictional demo catalog. Every row is fixed
 * (no faker randomness) so `migrate:fresh --seed` always yields the same
 * store; image URLs use deterministic placeholder seeds. No real personal
 * data appears anywhere in this data set.
 *
 * Seeding is idempotent (keyed on category name / product SKU), so it is
 * safe to re-run against an existing development database. AUTH-003 adds
 * the local/demo-only administrator via AdminUserSeeder.
 */
class DatabaseSeeder extends Seeder
{
    use WithoutModelEvents;

    /**
     * Seed the application's database.
     */
    public function run(): void
    {
        $this->seedCatalog();
        $this->seedTestCustomer();
        $this->call(AdminUserSeeder::class);
    }

    /**
     * Demo catalog: 5 categories (one archived), 12 products (one inactive,
     * one archived, one out of stock), and 1-3 ordered images each.
     */
    private function seedCatalog(): void
    {
        $categories = [
            ['name' => 'Audio & Headphones', 'sort_order' => 0, 'status' => 'active'],
            ['name' => 'Wearables', 'sort_order' => 1, 'status' => 'active'],
            ['name' => 'Desk & Workspace', 'sort_order' => 2, 'status' => 'active'],
            ['name' => 'Photography', 'sort_order' => 3, 'status' => 'active'],
            ['name' => 'Seasonal Clearance', 'sort_order' => 99, 'status' => 'archived'],
        ];

        foreach ($categories as $category) {
            Category::query()->updateOrCreate(['name' => $category['name']], $category);
        }

        $products = [
            // [name, description, price, sku, category, status, stock, imageCount]
            ['Aurora Wireless Headphones', 'Over-ear wireless headphones with active noise cancelling and 30-hour battery life.', '149.00', 'AH-1001', 'Audio & Headphones', 'active', 24, 3],
            ['Nimbus Earbuds Pro', 'Compact in-ear earbuds with spatial audio and a wireless charging case.', '89.50', 'AH-1002', 'Audio & Headphones', 'active', 40, 2],
            ['Studio Monitor Pair', 'Bookshelf studio monitors with balanced inputs for desktop listening.', '329.00', 'AH-1003', 'Audio & Headphones', 'active', 8, 2],
            ['Pulse Smartwatch', 'Fitness smartwatch with heart-rate tracking, GPS, and a seven-day battery.', '199.00', 'WA-2001', 'Wearables', 'active', 15, 3],
            ['Stride Activity Band', 'Slim activity band with sleep tracking and a lightweight silicone strap.', '59.00', 'WA-2002', 'Wearables', 'active', 0, 1],
            ['Solstice Running Watch', 'Outdoor running watch with route maps and a sunlight-readable display.', '249.00', 'WA-2003', 'Wearables', 'inactive', 12, 2],
            ['Halo Desk Lamp', 'Dimmable LED desk lamp with a warm-to-cool color range and USB-C passthrough.', '45.00', 'DW-3001', 'Desk & Workspace', 'active', 60, 2],
            ['Summit Ergonomic Chair', 'Mesh-back ergonomic chair with adjustable lumbar support and armrests.', '419.00', 'DW-3002', 'Desk & Workspace', 'active', 5, 3],
            ['Drift Walnut Desk Riser', 'Solid walnut monitor riser that lifts screens to eye level.', '79.00', 'DW-3003', 'Desk & Workspace', 'active', 18, 1],
            ['Lumen Mirrorless Camera', '24-megapixel mirrorless camera with in-body stabilization and a kit lens.', '899.00', 'PH-4001', 'Photography', 'active', 6, 3],
            ['Vista 35mm Prime Lens', 'Fast 35mm prime lens with a wide aperture for low-light photography.', '279.00', 'PH-4002', 'Photography', 'active', 10, 2],
            ['Clearance Gift Card Holder', 'Seasonal packaging leftover; no longer part of the active catalog.', '9.99', 'SC-9001', 'Seasonal Clearance', 'archived', 3, 1],
        ];

        foreach ($products as [$name, $description, $price, $sku, $category, $status, $stock, $imageCount]) {
            $product = Product::query()->updateOrCreate(
                ['sku' => $sku],
                [
                    'name' => $name,
                    'description' => $description,
                    'price' => $price,
                    'category_id' => Category::query()->where('name', $category)->firstOrFail()->id,
                    'status' => $status,
                ],
            );

            Inventory::query()->updateOrCreate(
                ['product_id' => $product->id],
                ['quantity' => $stock],
            );

            // Deterministic placeholder images; the seed string keeps every
            // URL stable across re-seeds.
            $existingImages = $product->images()->count();
            for ($i = $existingImages; $i < $imageCount; $i++) {
                ProductImage::query()->create([
                    'product_id' => $product->id,
                    'url' => 'https://picsum.photos/seed/'.strtolower($sku).'-'.($i + 1).'/800/800',
                    'sort_order' => $i,
                ]);
            }
        }
    }

    /**
     * A demo customer for manual login testing; not part of the catalog.
     */
    private function seedTestCustomer(): void
    {
        User::query()->firstOrCreate(
            ['email' => 'demo@example.com'],
            [
                'first_name' => 'Demo',
                'last_name' => 'Customer',
                'password' => 'Password123',
                'email_verified_at' => now(),
            ],
        );
    }
}
