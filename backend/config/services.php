<?php

return [

    /*
    |--------------------------------------------------------------------------
    | Third Party Services
    |--------------------------------------------------------------------------
    |
    | This file is for storing the credentials for third party services such
    | as Resend, Postmark, AWS, and more. This file provides the de facto
    | location for this type of information, allowing packages to have
    | a conventional file to locate the various service credentials.
    |
    */

    'postmark' => [
        'key' => env('POSTMARK_API_KEY'),
    ],

    'resend' => [
        'key' => env('RESEND_API_KEY'),
    ],

    'ses' => [
        'key' => env('AWS_ACCESS_KEY_ID'),
        'secret' => env('AWS_SECRET_ACCESS_KEY'),
        'region' => env('AWS_DEFAULT_REGION', 'us-east-1'),
    ],

    'slack' => [
        'notifications' => [
            'bot_user_oauth_token' => env('SLACK_BOT_USER_OAUTH_TOKEN'),
            'channel' => env('SLACK_BOT_USER_DEFAULT_CHANNEL'),
        ],
    ],

    'google' => [
        // AUTH-004/AUTH-B03: the OAuth client IDs whose `aud` claim
        // POST /auth/google accepts. `client_id` is the primary (Android) Web
        // application client; `client_ids` may list additional first-party
        // clients (web/iOS) as a comma-separated string. A token is accepted
        // when its audience matches any configured ID. No secret is required
        // to verify an ID token.
        'client_id' => env('GOOGLE_CLIENT_ID'),
        'client_ids' => env('GOOGLE_CLIENT_IDS', ''),
    ],

];
