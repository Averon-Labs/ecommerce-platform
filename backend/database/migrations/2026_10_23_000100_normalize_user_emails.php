<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Support\Collection;
use Illuminate\Support\Facades\DB;

/**
 * AUTH-001: emails are case-insensitive. Sign-in lowercases the submitted
 * address, so any account stored with capital letters could never sign in
 * again. Rewrite existing rows to the canonical lowercase form.
 */
return new class extends Migration
{
    public function up(): void
    {
        $duplicates = DB::table('users')
            ->selectRaw('LOWER(email) AS lower_email')
            ->groupByRaw('LOWER(email)')
            ->havingRaw('COUNT(*) > 1')
            ->pluck('lower_email');

        if ($duplicates instanceof Collection && $duplicates->isNotEmpty()) {
            // Merging accounts is a human decision; fail loudly instead of
            // silently deleting or renaming one of the colliding rows.
            throw new RuntimeException(
                'Cannot normalize user emails: these addresses differ only by '
                .'letter case and must be resolved manually first: '
                .$duplicates->implode(', '),
            );
        }

        // Compare in PHP: under MySQL's utf8mb4_unicode_ci collation a SQL
        // `email <> LOWER(email)` predicate is always false, which would
        // make this migration a silent no-op on the project database.
        DB::table('users')
            ->select('id', 'email')
            ->orderBy('id')
            ->chunkById(100, function (Collection $users): void {
                foreach ($users as $user) {
                    $normalized = strtolower(trim((string) $user->email));

                    if ($user->email !== $normalized) {
                        DB::table('users')
                            ->where('id', $user->id)
                            ->update(['email' => $normalized]);
                    }
                }
            });
    }

    /**
     * The original letter casing cannot be recovered.
     */
    public function down(): void
    {
        //
    }
};
