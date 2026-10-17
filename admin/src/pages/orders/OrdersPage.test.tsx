import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import App from '../../App';
import { jsonResponse, mockApi } from '../../test/apiMock';
import type { MockRoute } from '../../test/apiMock';
import { orderSummary, pagination, sessionRoute } from '../../test/adminMock';

function ordersRoute(handler: MockRoute['handler']): MockRoute {
  return { method: 'GET', path: '/admin/orders', handler };
}

async function findOrdersTable() {
  return screen.findByRole('table', { name: 'Orders' });
}

beforeEach(() => {
  window.history.pushState({}, '', '/orders');
  window.localStorage.clear();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('OrdersPage (A-06)', () => {
  it('orders_list_renders_reference_customer_total_and_statuses', async () => {
    mockApi([
      sessionRoute(),
      ordersRoute(() =>
        jsonResponse(200, { data: [orderSummary()], pagination: pagination(1, 20, 1, 1) }),
      ),
    ]);

    render(<App />);

    const table = await findOrdersTable();
    expect(within(table).getByText('ESV-000017')).toBeInTheDocument();
    expect(within(table).getByText('Sara Nouri')).toBeInTheDocument();
    expect(within(table).getByText('sara@example.com')).toBeInTheDocument();
    expect(within(table).getByText('$84.98')).toBeInTheDocument();
    expect(within(table).getByText('Confirmed')).toBeInTheDocument();
    expect(within(table).getByText('Completed')).toBeInTheDocument();
    expect(within(table).getByRole('link', { name: 'View order ESV-000017' })).toHaveAttribute(
      'href',
      '/orders/17',
    );
  });

  it('orders_empty_state_points_to_products', async () => {
    mockApi([
      sessionRoute(),
      ordersRoute(() => jsonResponse(200, { data: [], pagination: pagination() })),
    ]);

    render(<App />);

    const emptyTitle = await screen.findByText('No orders yet');
    expect(
      within(emptyTitle.parentElement as HTMLElement).getByRole('link', { name: 'View products' }),
    ).toHaveAttribute('href', '/products');
  });

  it('orders_no_results_state_clears_filters_and_reloads', async () => {
    mockApi([
      sessionRoute(),
      ordersRoute((request) =>
        request.url.includes('q=')
          ? jsonResponse(200, { data: [], pagination: pagination() })
          : jsonResponse(200, { data: [orderSummary()], pagination: pagination(1, 20, 1, 1) }),
      ),
    ]);

    render(<App />);

    const table = await findOrdersTable();
    expect(within(table).getByText('ESV-000017')).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText('Search orders'), { target: { value: 'zzz' } });

    const emptyTitle = await screen.findByText('No orders match your filters');
    fireEvent.click(
      within(emptyTitle.parentElement as HTMLElement).getByRole('button', {
        name: 'Clear filters',
      }),
    );

    const reloaded = await findOrdersTable();
    expect(within(reloaded).getByText('ESV-000017')).toBeInTheDocument();
    expect(screen.getByLabelText('Search orders')).toHaveValue('');
  });

  it('orders_error_state_retries_and_recovers', async () => {
    let calls = 0;
    mockApi([
      sessionRoute(),
      ordersRoute(() => {
        calls += 1;
        return calls === 1
          ? jsonResponse(500, {
              error: { code: 'INTERNAL_ERROR', message: 'The server failed to handle the request.' },
            })
          : jsonResponse(200, { data: [orderSummary()], pagination: pagination(1, 20, 1, 1) });
      }),
    ]);

    render(<App />);

    expect(await screen.findByText('Orders could not be loaded')).toBeInTheDocument();
    expect(screen.getByText('The server failed to handle the request.')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));

    const table = await findOrdersTable();
    expect(within(table).getByText('ESV-000017')).toBeInTheDocument();
  });

  it('orders_filters_are_sent_to_the_api', async () => {
    const urls: string[] = [];
    mockApi([
      sessionRoute(),
      ordersRoute((request) => {
        urls.push(request.url);
        return jsonResponse(200, { data: [], pagination: pagination() });
      }),
    ]);

    render(<App />);

    await screen.findByText('No orders yet');

    fireEvent.change(screen.getByLabelText('Search orders'), { target: { value: 'sara' } });
    fireEvent.change(screen.getByLabelText('Status'), { target: { value: 'processing' } });
    fireEvent.change(screen.getByLabelText('From date'), { target: { value: '2026-10-01' } });
    fireEvent.change(screen.getByLabelText('To date'), { target: { value: '2026-10-31' } });

    await waitFor(() => {
      expect(
        urls.some(
          (url) =>
            url.includes('q=sara') &&
            url.includes('status=processing') &&
            url.includes('from=2026-10-01') &&
            url.includes('to=2026-10-31'),
        ),
      ).toBe(true);
    });
  });

  it('orders_pagination_follows_api_metadata', async () => {
    mockApi([
      sessionRoute(),
      ordersRoute((request) =>
        request.url.includes('page=2')
          ? jsonResponse(200, {
              data: [orderSummary({ id: 18, reference: 'ESV-000018' })],
              pagination: pagination(2, 20, 21, 2),
            })
          : jsonResponse(200, {
              data: [orderSummary()],
              pagination: pagination(1, 20, 21, 2),
            }),
      ),
    ]);

    render(<App />);

    await findOrdersTable();
    const firstNav = await screen.findByRole('navigation', { name: 'Pagination' });
    expect(within(firstNav).getByText('Page 1 of 2')).toBeInTheDocument();
    expect(within(firstNav).getByText('21 orders')).toBeInTheDocument();
    expect(within(firstNav).getByRole('button', { name: 'Previous' })).toBeDisabled();

    fireEvent.click(within(firstNav).getByRole('button', { name: 'Next' }));

    await waitFor(() => {
      const table = screen.getByRole('table', { name: 'Orders' });
      expect(within(table).getByText('ESV-000018')).toBeInTheDocument();
    });
    const secondNav = screen.getByRole('navigation', { name: 'Pagination' });
    expect(within(secondNav).getByText('Page 2 of 2')).toBeInTheDocument();
    expect(within(secondNav).getByRole('button', { name: 'Next' })).toBeDisabled();
  });

  it('orders_list_401_ends_the_session_and_returns_to_sign_in', async () => {
    mockApi([
      sessionRoute(),
      ordersRoute(() =>
        jsonResponse(401, {
          error: { code: 'UNAUTHENTICATED', message: 'Authentication is required.' },
        }),
      ),
    ]);

    render(<App />);

    expect(await screen.findByRole('heading', { name: 'Admin sign in' })).toBeInTheDocument();
  });
});