import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import App from '../../App';
import { jsonResponse, mockApi } from '../../test/apiMock';
import type { MockRoute } from '../../test/apiMock';
import { salesReport, sessionRoute } from '../../test/adminMock';

function reportsRoute(handler: MockRoute['handler']): MockRoute {
  return { method: 'GET', path: '/admin/reports/sales', handler };
}

beforeEach(() => {
  window.history.pushState({}, '', '/reports');
  window.localStorage.clear();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('ReportsPage (RPT-002)', () => {
  it('reports_page_renders_summary_chart_and_rankings', async () => {
    mockApi([
      sessionRoute(),
      reportsRoute(() => jsonResponse(200, { data: salesReport() })),
    ]);

    render(<App />);

    expect(await screen.findByRole('heading', { name: 'Sales reports' })).toBeInTheDocument();
    expect(await screen.findByText('$95.00')).toBeInTheDocument();
    const paidOrders = screen.getByText('Paid orders', { selector: 'dt' });
    expect(within(paidOrders.parentElement as HTMLElement).getByText('2')).toBeInTheDocument();
    expect(screen.getByText('$47.50')).toBeInTheDocument();

    const chart = screen.getByRole('figure', { name: /Daily revenue/ });
    expect(within(chart).getByRole('table', { name: 'Daily revenue values' })).toBeInTheDocument();
    expect(within(chart).getByText('2026-10-28')).toBeInTheDocument();
    expect(within(chart).getByText('$25.00')).toBeInTheDocument();

    const products = screen.getByRole('table', { name: 'Top products by revenue' });
    expect(within(products).getByText('Aurora Desk Lamp')).toBeInTheDocument();
    expect(within(products).getByText('$65.00')).toBeInTheDocument();

    const statuses = screen.getByRole('list', { name: 'Orders by status' });
    expect(within(statuses).getByText('Pending payment')).toBeInTheDocument();
    expect(within(statuses).getByText('Cancelled')).toBeInTheDocument();

    expect(screen.getByRole('link', { name: 'Reports' })).toHaveAttribute('href', '/reports');
  });

  it('reports_presets_and_custom_dates_request_the_server', async () => {
    const urls: string[] = [];
    mockApi([
      sessionRoute(),
      reportsRoute((request) => {
        urls.push(request.url);
        return jsonResponse(200, { data: salesReport() });
      }),
    ]);

    render(<App />);

    await screen.findByText('$95.00');
    const initialRequests = urls.length;

    fireEvent.click(screen.getByRole('button', { name: 'Last 7 days' }));

    await waitFor(() => expect(urls.length).toBeGreaterThan(initialRequests));
    expect(urls[urls.length - 1]).toContain('from=');
    expect(urls[urls.length - 1]).toContain('to=');
    expect(screen.getByRole('button', { name: 'Last 7 days' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );

    fireEvent.change(screen.getByLabelText('From date'), { target: { value: '2026-10-01' } });
    fireEvent.change(screen.getByLabelText('To date'), { target: { value: '2026-10-15' } });
    fireEvent.click(screen.getByRole('button', { name: 'Apply range' }));

    await waitFor(() => {
      expect(
        urls.some((url) => url.includes('from=2026-10-01') && url.includes('to=2026-10-15')),
      ).toBe(true);
    });
  });

  it('reports_custom_range_rejects_from_after_to_without_a_request', async () => {
    const urls: string[] = [];
    mockApi([
      sessionRoute(),
      reportsRoute((request) => {
        urls.push(request.url);
        return jsonResponse(200, { data: salesReport() });
      }),
    ]);

    render(<App />);

    await screen.findByText('$95.00');
    const requestCount = urls.length;

    fireEvent.change(screen.getByLabelText('From date'), { target: { value: '2026-10-20' } });
    fireEvent.change(screen.getByLabelText('To date'), { target: { value: '2026-10-10' } });
    fireEvent.click(screen.getByRole('button', { name: 'Apply range' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'The from date must not be after the to date.',
    );
    expect(urls).toHaveLength(requestCount);
  });

  it('reports_empty_state_explains_the_selected_range', async () => {
    mockApi([
      sessionRoute(),
      reportsRoute(() =>
        jsonResponse(200, {
          data: salesReport({
            summary: {
              revenue: '0.00',
              totalOrders: 0,
              paidOrders: 0,
              itemsSold: 0,
              averageOrderValue: '0.00',
            },
            topProducts: [],
          }),
        }),
      ),
    ]);

    render(<App />);

    expect(await screen.findByText('No orders in this range')).toBeInTheDocument();
    expect(screen.queryByRole('figure', { name: /Daily revenue/ })).not.toBeInTheDocument();
  });

  it('reports_error_state_retries_and_recovers', async () => {
    let calls = 0;

    mockApi([
      sessionRoute(),
      reportsRoute(() => {
        calls += 1;
        return calls === 1
          ? jsonResponse(500, {
              error: { code: 'INTERNAL_ERROR', message: 'The server failed to handle the request.' },
            })
          : jsonResponse(200, { data: salesReport() });
      }),
    ]);

    render(<App />);

    expect(await screen.findByText('Sales report could not be loaded')).toBeInTheDocument();
    expect(screen.getByText('The server failed to handle the request.')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));

    expect(await screen.findByText('$95.00')).toBeInTheDocument();
  });

  it('reports_401_ends_the_session_and_returns_to_sign_in', async () => {
    mockApi([
      sessionRoute(),
      reportsRoute(() =>
        jsonResponse(401, {
          error: { code: 'UNAUTHENTICATED', message: 'Authentication is required.' },
        }),
      ),
    ]);

    render(<App />);

    expect(await screen.findByRole('heading', { name: 'Admin sign in' })).toBeInTheDocument();
  });

  it('reports_loading_state_shows_a_fixed_height_skeleton', async () => {
    const control: { resolve: (() => void) | null } = { resolve: null };

    mockApi([
      sessionRoute(),
      reportsRoute(
        () =>
          new Promise<Response>((resolve) => {
            control.resolve = () => resolve(jsonResponse(200, { data: salesReport() }));
          }),
      ),
    ]);

    render(<App />);

    expect(await screen.findByRole('status', { name: 'Loading sales report' })).toBeInTheDocument();
    expect(screen.queryByText('$95.00')).not.toBeInTheDocument();

    await waitFor(() => expect(control.resolve).not.toBeNull());
    control.resolve?.();

    expect(await screen.findByText('$95.00')).toBeInTheDocument();
    expect(screen.queryByRole('status', { name: 'Loading sales report' })).not.toBeInTheDocument();
  });
});
