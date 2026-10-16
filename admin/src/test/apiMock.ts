import { vi } from 'vitest';

export interface MockRequest {
  url: string;
  method: string;
  path: string;
  body: unknown;
  headers: Record<string, string>;
}

export interface MockRoute {
  method: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  path: string;
  handler: (request: MockRequest) => Response | Promise<Response>;
}

/**
 * Stub `fetch` with exact `METHOD path` routes so tests exercise the real
 * HTTP client (headers, credentials, error envelope) without a backend.
 * Unexpected requests fail loudly instead of silently hitting the network.
 */
export function mockApi(routes: MockRoute[]) {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init: RequestInit = {}) => {
    const url =
      typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url;
    const method = (init.method ?? 'GET').toUpperCase();
    const path = new URL(url).pathname;
    // Routes are declared without the `/api/v1` base; match on the suffix.
    const route = routes.find(
      (candidate) => candidate.method === method && path.endsWith(candidate.path),
    );

    if (!route) {
      throw new Error(`Unexpected API request in test: ${method} ${path}`);
    }

    return route.handler({
      url,
      method,
      path,
      body: typeof init.body === 'string' ? JSON.parse(init.body) : undefined,
      headers: (init.headers as Record<string, string> | undefined) ?? {},
    });
  });

  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

export function jsonResponse(
  status: number,
  body?: unknown,
  headers: Record<string, string> = {},
): Response {
  return new Response(body === undefined ? null : JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json', ...headers },
  });
}
