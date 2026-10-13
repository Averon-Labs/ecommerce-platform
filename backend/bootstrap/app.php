<?php

use App\Support\ApiError;
use Illuminate\Auth\AuthenticationException;
use Illuminate\Foundation\Application;
use Illuminate\Foundation\Configuration\Exceptions;
use Illuminate\Foundation\Configuration\Middleware;
use Illuminate\Http\Exceptions\ThrottleRequestsException;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Validation\ValidationException;
use Symfony\Component\HttpKernel\Exception\HttpExceptionInterface;
use Symfony\Component\HttpKernel\Exception\NotFoundHttpException;

return Application::configure(basePath: dirname(__DIR__))
    ->withRouting(
        web: __DIR__.'/../routes/web.php',
        api: __DIR__.'/../routes/api.php',
        apiPrefix: 'api/v1',
        commands: __DIR__.'/../routes/console.php',
        health: '/up',
    )
    ->withMiddleware(function (Middleware $middleware): void {
        //
    })
    ->withExceptions(function (Exceptions $exceptions): void {
        $exceptions->shouldRenderJsonWhen(
            fn (Request $request) => $request->is('api/*') || $request->expectsJson(),
        );

        $exceptions->render(function (ValidationException $e, Request $request): ?JsonResponse {
            if (! $request->is('api/*')) {
                return null;
            }

            $details = collect($e->errors())
                ->flatMap(fn (array $messages, string $field) => collect($messages)->map(
                    fn (string $message) => ['field' => $field, 'message' => $message],
                ))
                ->values()
                ->all();

            return ApiError::response(422, 'VALIDATION_ERROR', 'One or more request fields are invalid.', $details);
        });

        $exceptions->render(function (AuthenticationException $e, Request $request): ?JsonResponse {
            if (! $request->is('api/*')) {
                return null;
            }

            return ApiError::response(401, 'UNAUTHENTICATED', 'Authentication is required for this endpoint.');
        });

        $exceptions->render(function (NotFoundHttpException $e, Request $request): ?JsonResponse {
            if (! $request->is('api/*')) {
                return null;
            }

            return ApiError::response(404, 'NOT_FOUND', 'The requested resource was not found.');
        });

        $exceptions->render(function (ThrottleRequestsException $e, Request $request): ?JsonResponse {
            if (! $request->is('api/*')) {
                return null;
            }

            return ApiError::response(
                429,
                'RATE_LIMITED',
                'Too many requests. Please slow down and retry later.',
                [],
                $e->getHeaders(),
            );
        });

        $exceptions->render(function (Throwable $e, Request $request): ?JsonResponse {
            if (! $request->is('api/*') || $e instanceof HttpExceptionInterface) {
                return null;
            }

            report($e);

            return ApiError::response(500, 'INTERNAL_ERROR', 'An unexpected error occurred.');
        });
    })->create();
