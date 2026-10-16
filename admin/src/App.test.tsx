import { render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import App from './App';
import { jsonResponse, mockApi } from './test/apiMock';

beforeEach(() => {
  window.history.pushState({}, '', '/');
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('App', () => {
  it('renders_admin_shell_without_dom_leakage_between_tests', () => {
    // src/setupTests.ts runs cleanup after each test.
    expect(document.body).toBeEmptyDOMElement();
  });

  it('renders_the_guest_boot_flow', async () => {
    mockApi([
      {
        method: 'GET',
        path: '/admin/auth/session',
        handler: () =>
          jsonResponse(401, {
            error: { code: 'UNAUTHENTICATED', message: 'Authentication is required.' },
          }),
      },
    ]);

    render(<App />);

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Admin sign in' }),
    ).toBeInTheDocument();
  });
});
