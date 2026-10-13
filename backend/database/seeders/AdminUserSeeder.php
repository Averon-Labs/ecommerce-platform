<?php

namespace Database\Seeders;

use App\Models\User;
use Illuminate\Database\Seeder;
use Illuminate\Support\Str;

/**
 * AUTH-003 backend portion: the demo administrator used by the admin
 * dashboard in local/demo environments (to be documented in the handoff
 * guide, QUAL-004). The account never ships a reusable public credential:
 * the password comes from DEMO_ADMIN_PASSWORD (backend/.env, git-ignored)
 * and is generated randomly for that install when the variable is unset.
 */
class AdminUserSeeder extends Seeder
{
    /** Local/demo-only sign-in address; not a real mailbox. */
    private const EMAIL = 'demo-admin@eshopverse.test';

    public function run(): void
    {
        $password = (string) env('DEMO_ADMIN_PASSWORD', '');

        $user = $password !== ''
            ? User::query()->updateOrCreate(
                ['email' => self::EMAIL],
                [
                    'first_name' => 'Demo',
                    'last_name' => 'Administrator',
                    'password' => $password,
                    'email_verified_at' => now(),
                ],
            )
            : User::query()->firstOrCreate(
                ['email' => self::EMAIL],
                [
                    'first_name' => 'Demo',
                    'last_name' => 'Administrator',
                    // Random per local install: with no DEMO_ADMIN_PASSWORD
                    // there is no shared secret that could be reused.
                    'password' => Str::password(24),
                    'email_verified_at' => now(),
                ],
            );

        // role/status are guarded by the model's fillable list, and this
        // is exactly the one place the API cannot escalate: the seeder.
        if ($user->role !== 'admin' || $user->status !== 'active') {
            $user->forceFill(['role' => 'admin', 'status' => 'active'])->save();
        }

        $this->command?->info(
            'Demo admin: '.self::EMAIL
            .($password !== ''
                ? ' (password from DEMO_ADMIN_PASSWORD).'
                : ' (random password — set DEMO_ADMIN_PASSWORD in backend/.env and re-run to choose one).'),
        );
    }
}
