import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import App from '../../App';
import { jsonResponse, mockApi } from '../../test/apiMock';
import type { MockRoute, MockRequest } from '../../test/apiMock';
import { orderDetail, sessionRoute } from '../../test/adminMock';

function detailRoute(handler: MockRoute['handler']): MockRoute {
  return { method: 'GET', path: '/admin/orders/17', handler };
}

async function findStatusSelect() {
  return screen.findByLabelText('Next status');
}

beforeEach(() => {
  window.history.pushState({}, '', '/orders/17');
  window.localStorage.clear();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('OrderDetailPage (A-07)', () => {
  it('order_detail_renders_snapshots_payment_and_timeline', async () => {
    mockApi([sessionRoute(), detailRoute(() => jsonResponse(200, { data: orderDetail() }))]);

    render(<App />);

    expect(
      await screen.findByRole('heading', { name: 'ESV-000017' }),
    ).toBeInTheDocument();
    expect(screen.getByText('sara@example.com')).toBeInTheDocument();
    expect(screen.getByText('14 Maple Street')).toBeInTheDocument();
    expect(screen.getByText('Seattle, WA 98101')).toBeInTheDocument();
    expect(screen.getByText('Please leave at the front desk.')).toBeInTheDocument();

    const items = screen.getByRole('table', { name: 'Order items' });
    expect(within(items).getByText('Aurora Desk Lamp')).toBeInTheDocument();
    expect(within(items).getByText('2')).toBeInTheDocument();
    expect(within(items).getByText('$39.99')).toBeInTheDocument();
    expect(within(items).getAllByText('$79.98').length).toBeGreaterThan(0);

    expect(screen.getByText('Sample shipping')).toBeInTheDocument();
    expect(screen.getByText('$5.00')).toBeInTheDocument();
    expect(screen.getAllByText('$84.98').length).toBeGreaterThan(0);
    expect(screen.getByText('DEMO-8HZK4M2QW7PR')).toBeInTheDocument();

    const timeline = screen.getByText('Timeline');
    expect(timeline).toBeInTheDocument();
    expect(screen.getAllByText('Pending payment').length).toBeGreaterThan(0);
  });

  it('status_action_offers_only_the_server_permitted_transitions', async () => {
    mockApi([
      sessionRoute(),
      detailRoute(() =>
        jsonResponse(200, {
          data: orderDetail({ allowedTransitions: ['shipped', 'cancelled'] }),
        }),
      ),
    ]);

    render(<App />);

    const select = await findStatusSelect();
    const options = within(select)
      .getAllByRole('option')
      .map((option) => option.textContent);
    expect(options).toEqual(['Shipped', 'Cancelled']);
  });

  it('terminal_order_shows_no_status_action', async () => {
    mockApi([
      sessionRoute(),
      detailRoute(() =>
        jsonResponse(200, {
          data: orderDetail({ status: 'delivered', allowedTransitions: [] }),
        }),
      ),
    ]);

    render(<App />);

    expect(await screen.findByText(/terminal state/)).toBeInTheDocument();
    expect(screen.queryByLabelText('Next status')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Update status' })).not.toBeInTheDocument();
  });

  it('transition_confirms_and_never_claims_success_before_the_server_responds', async () => {
    const patchControl: { resolve: (() => void) | null } = { resolve: null };
    let patchBody: unknown = null;

    mockApi([
      sessionRoute(),
      detailRoute(() =>
        jsonResponse(200, {
          data: orderDetail({ status: 'processing', allowedTransitions: ['shipped', 'cancelled'] }),
        }),
      ),
      {
        method: 'PATCH',
        path: '/admin/orders/17/status',
        handler: (request: MockRequest) => {
          patchBody = request.body;
          return new Promise<Response>((resolve) => {
            patchControl.resolve = () =>
              resolve(
                jsonResponse(200, {
                  data: orderDetail({
                    status: 'shipped',
                    allowedTransitions: ['delivered'],
                    timeline: [
                      { status: 'pending_payment', at: '2026-10-11T09:41:00Z' },
                      { status: 'confirmed', at: '2026-10-11T09:41:05Z' },
                      { status: 'shipped', at: '2026-10-11T10:05:00Z' },
                    ],
                  }),
                }),
              );
          });
        },
      },
    ]);

    render(<App />);

    const select = await findStatusSelect();
    fireEvent.change(select, { target: { value: 'shipped' } });
    fireEvent.click(screen.getByRole('button', { name: 'Update status' }));

    const dialog = await screen.findByRole('dialog', { name: 'Move ESV-000017 to Shipped?' });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Move to Shipped' }));

    await waitFor(() => expect(patchBody).toEqual({ status: 'shipped' }));

    // The response has not resolved yet: no success claim, no new option.
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    expect(within(select).queryByRole('option', { name: 'Delivered' })).not.toBeInTheDocument();

    patchControl.resolve?.();

    expect(await screen.findByRole('status')).toHaveTextContent('ESV-000017 is now shipped.');
    await waitFor(() => {
      expect(within(select).getByRole('option', { name: 'Delivered' })).toBeInTheDocument();
      expect(within(select).queryByRole('option', { name: 'Shipped' })).not.toBeInTheDocument();
    });
  });

  it('invalid_transition_conflict_explains_and_offers_refresh', async () => {
    let loads = 0;

    mockApi([
      sessionRoute(),
      detailRoute(() => {
        loads += 1;
        return jsonResponse(200, {
          data:
            loads === 1
              ? orderDetail({ status: 'confirmed', allowedTransitions: ['processing'] })
              : orderDetail({ status: 'processing', allowedTransitions: ['shipped', 'cancelled'] }),
        });
      }),
      {
        method: 'PATCH',
        path: '/admin/orders/17/status',
        handler: () =>
          jsonResponse(409, {
            error: {
              code: 'INVALID_TRANSITION',
              message: 'An order with status confirmed cannot become shipped.',
            },
          }),
      },
    ]);

    render(<App />);

    const select = await findStatusSelect();
    fireEvent.change(select, { target: { value: 'processing' } });
    fireEvent.click(screen.getByRole('button', { name: 'Update status' }));
    const dialog = await screen.findByRole('dialog', { name: 'Move ESV-000017 to Processing?' });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Move to Processing' }));

    expect(
      await screen.findByText('An order with status confirmed cannot become shipped.'),
    ).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Refresh order' }));

    await waitFor(() => expect(loads).toBeGreaterThan(1));
    await waitFor(() => {
      expect(screen.getAllByText('Processing').length).toBeGreaterThan(0);
    });
  });

  it('order_detail_not_found_offers_back_to_orders', async () => {
    mockApi([
      sessionRoute(),
      detailRoute(() =>
        jsonResponse(404, {
          error: { code: 'NOT_FOUND', message: 'The requested order was not found.' },
        }),
      ),
    ]);

    render(<App />);

    expect(await screen.findByText('Order not found')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Back to orders' })).toHaveAttribute(
      'href',
      '/orders',
    );
  });

  it('order_detail_error_state_retries_and_recovers', async () => {
    let calls = 0;
    mockApi([
      sessionRoute(),
      detailRoute(() => {
        calls += 1;
        return calls === 1
          ? jsonResponse(500, {
              error: { code: 'INTERNAL_ERROR', message: 'The server failed to handle the request.' },
            })
          : jsonResponse(200, { data: orderDetail() });
      }),
    ]);

    render(<App />);

    expect(await screen.findByText('Order could not be loaded')).toBeInTheDocument();
    expect(screen.getByText('The server failed to handle the request.')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));

    expect(await screen.findByRole('heading', { name: 'ESV-000017' })).toBeInTheDocument();
  });

  it('order_detail_401_ends_the_session_and_returns_to_sign_in', async () => {
    mockApi([
      sessionRoute(),
      detailRoute(() =>
        jsonResponse(401, {
          error: { code: 'UNAUTHENTICATED', message: 'Authentication is required.' },
        }),
      ),
    ]);

    render(<App />);

    expect(await screen.findByRole('heading', { name: 'Admin sign in' })).toBeInTheDocument();
  });
});