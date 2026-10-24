<?php

namespace Tests\Feature;

use Tests\TestCase;

class ApiRoutingTest extends TestCase
{
    /**
     * The API must only expose routes that exist in the reviewed contract.
     * Until FND-004 is accepted, routes/api.php is empty, so an unknown
     * v1 path has to be rejected instead of falling through to the web app.
     */
    public function test_get_unknown_api_v1_path_returns404(): void
    {
        $response = $this->getJson('/api/v1/definitely-not-a-route');

        $response->assertStatus(404);
    }

    public function test_wrong_method_on_api_v1_path_returns405_error_envelope(): void
    {
        $response = $this->postJson('/api/v1/products');

        $this->assertApiError($response, 405, 'METHOD_NOT_ALLOWED');
    }
}
