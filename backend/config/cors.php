<?php

/*
|--------------------------------------------------------------------------
| CORS for the first-party admin SPA (AUTH-003 / CAT-004)
|--------------------------------------------------------------------------
|
| The admin dashboard runs on the Vite development server (:5173) while
| the API runs on the Laravel development server (:8000), so its requests
| are cross-origin. Because the SPA authenticates with the stateful
| Sanctum session cookie, the response must echo a specific allowed
| origin and allow credentials — a wildcard origin is not valid for
| credentialed requests.
|
| Local defaults cover the documented SPA origins only. Deployments set
| CORS_ALLOWED_ORIGINS to the deployed admin origin. No credentials are
| configured here.
|
*/

return [
    'paths' => ['api/*', 'sanctum/csrf-cookie'],

    'allowed_methods' => ['*'],

    'allowed_origins' => array_values(array_filter(array_map(
        'trim',
        explode(',', (string) env(
            'CORS_ALLOWED_ORIGINS',
            'http://localhost:5173,http://127.0.0.1:5173,http://localhost:5174,http://127.0.0.1:5174',
        )),
    ))),

    'allowed_origins_patterns' => [],

    'allowed_headers' => ['*'],

    'exposed_headers' => [],

    'max_age' => 0,

    'supports_credentials' => true,
];
