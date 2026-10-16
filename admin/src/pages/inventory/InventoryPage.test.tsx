import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import App from '../../App';
import { jsonResponse, mockApi } from '../../test/apiMock';
import type { MockRoute } from '../../test/apiMock';
import { inventoryRow, pagination, sessionRoute } from '../../test/adminMock';

function inventoryRoute(handler: MockRoute['handler']): MockRoute {
  return { method: 'GET', path: '/admin/inventory', handler };
}

async function findInventoryTable() {
  return screen.findByRole('table', { name: 'Inventory' });
}

beforeEach(() => {
  window.history.pushState({}, '', '/inventory');
  window.localStorage.clear();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('InventoryPage (A-05)', () => {
  it('inventory_list_renders_quantity_and_availability', async () => {
    mockApi([
      sessionRoute(),
      inventoryRoute(() =>
        jsonResponse(200, { data: [inventoryRow()], pagination: pagination(1, 20, 1, 1) }),
      ),
    ]);

    render(<App />);

    const table = await findInventoryTable();
    expect(within(table).getByText('Aurora Desk Lamp')).toBeInTheDocument();
    expect(within(table).getByText('7')).toBeInTheDocument();
    expect(within(table).getByText('In stock')).toBeInTheDocument();
    expect(
      within(table).getByRole('button', { name: 'Adjust stock for Aurora Desk Lamp' }),
    ).toBeInTheDocument();
  });

  it('availability_filter_requests_out_of_stock_rows', async () => {
    const urls: string[] = [];
    mockApi([
      sessionRoute(),
      inventoryRoute((request) => {
        urls.push(request.url);
        const filtered = request.url.includes('inStock=false');
        return jsonResponse(200, {
          data: filtered
            ? [inventoryRow({ quantity: 0, inStock: false })]
            : [
                inventoryRow(),
                inventoryRow({ productId: 6, name: 'Oak Shelf', sku: 'OAK-1', quantity: 0, inStock: false }),
              ],
          pagination: pagination(1, 20, 1, 1),
        });
      }),
    ]);

    render(<App />);

    await findInventoryTable();
    fireEvent.change(screen.getByLabelText('Availability'), { target: { value: 'out-of-stock' } });

    await waitFor(() => {
      const table = screen.getByRole('table', { name: 'Inventory' });
      expect(within(table).queryByText('7')).not.toBeInTheDocument();
      expect(within(table).getByText('Out of stock')).toBeInTheDocument();
    });
    expect(urls.some((url) => url.includes('inStock=false'))).toBe(true);
  });

  it('adjust_stock_validates_and_saves_the_absolute_quantity', async () => {
    let quantity = 4;
    const fetchMock = mockApi([
      sessionRoute(),
      inventoryRoute(() =>
        jsonResponse(200, {
          data: [inventoryRow({ quantity, inStock: quantity > 0 })],
          pagination: pagination(1, 20, 1, 1),
        }),
      ),
      {
        method: 'PATCH',
        path: '/admin/inventory/5',
        handler: () => {
          quantity = 10;
          return jsonResponse(200, { data: inventoryRow({ quantity: 10, inStock: true }) });
        },
      },
    ]);

    render(<App />);

    const table = await findInventoryTable();
    fireEvent.click(within(table).getByRole('button', { name: 'Adjust stock for Aurora Desk Lamp' }));

    const dialog = await screen.findByRole('dialog', { name: /adjust stock/i });
    expect(within(dialog).getByText(/Current quantity:/)).toBeInTheDocument();

    fireEvent.change(within(dialog).getByLabelText('New quantity'), { target: { value: '-1' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Save quantity' }));

    expect(await screen.findByText('Enter a whole number of 0 or more.')).toBeInTheDocument();
    expect(
      fetchMock.mock.calls.filter(
        ([, init]) => (init as RequestInit | undefined)?.method === 'PATCH',
      ),
    ).toHaveLength(0);

    fireEvent.change(within(dialog).getByLabelText('New quantity'), { target: { value: '10' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Save quantity' }));

    expect(await screen.findByRole('status')).toHaveTextContent(
      'Stock for Aurora Desk Lamp is now 10.',
    );

    await waitFor(() => {
      const table = screen.getByRole('table', { name: 'Inventory' });
      expect(within(table).getByText('10')).toBeInTheDocument();
    });
  });

  it('inventory_empty_state_links_to_add_product', async () => {
    mockApi([
      sessionRoute(),
      inventoryRoute(() => jsonResponse(200, { data: [], pagination: pagination() })),
    ]);

    render(<App />);

    expect(await screen.findByText('No inventory yet')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Add a product' })).toHaveAttribute(
      'href',
      '/products/new',
    );
  });
});
