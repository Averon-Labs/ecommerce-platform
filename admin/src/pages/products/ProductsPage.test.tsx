import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import App from '../../App';
import { jsonResponse, mockApi } from '../../test/apiMock';
import type { MockRoute } from '../../test/apiMock';
import { category, pagination, product, sessionRoute } from '../../test/adminMock';

function productsRoute(handler: MockRoute['handler']): MockRoute {
  return { method: 'GET', path: '/admin/products', handler };
}

function categoryListRoute(items = [category()]): MockRoute {
  return {
    method: 'GET',
    path: '/admin/categories',
    handler: () => jsonResponse(200, { data: items }),
  };
}

async function findProductsTable() {
  return screen.findByRole('table', { name: 'Products' });
}

beforeEach(() => {
  window.history.pushState({}, '', '/products');
  window.localStorage.clear();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('ProductsPage (A-02)', () => {
  it('products_list_renders_status_stock_and_price_from_the_api', async () => {
    mockApi([
      sessionRoute(),
      categoryListRoute(),
      productsRoute(() =>
        jsonResponse(200, { data: [product()], pagination: pagination(1, 20, 1, 1) }),
      ),
    ]);

    render(<App />);

    const table = await findProductsTable();
    expect(within(table).getByText('Aurora Desk Lamp')).toBeInTheDocument();
    expect(within(table).getByText('$39.99')).toBeInTheDocument();
    expect(within(table).getByText('7 in stock')).toBeInTheDocument();
    expect(within(table).getByText('Active')).toBeInTheDocument();
    expect(within(table).getByRole('link', { name: 'Edit Aurora Desk Lamp' })).toBeInTheDocument();
    expect(
      within(table).getByRole('button', { name: 'Archive Aurora Desk Lamp' }),
    ).toBeInTheDocument();
  });

  it('products_empty_catalog_offers_add_product', async () => {
    mockApi([
      sessionRoute(),
      categoryListRoute([]),
      productsRoute(() => jsonResponse(200, { data: [], pagination: pagination() })),
    ]);

    render(<App />);

    const emptyTitle = await screen.findByText('No products yet');
    expect(
      within(emptyTitle.parentElement as HTMLElement).getByRole('link', { name: 'Add product' }),
    ).toHaveAttribute('href', '/products/new');
  });

  it('products_no_results_state_clears_filters_and_reloads', async () => {
    mockApi([
      sessionRoute(),
      categoryListRoute(),
      productsRoute((request) =>
        request.url.includes('q=')
          ? jsonResponse(200, { data: [], pagination: pagination() })
          : jsonResponse(200, { data: [product()], pagination: pagination(1, 20, 1, 1) }),
      ),
    ]);

    render(<App />);

    const table = await findProductsTable();
    expect(within(table).getByText('Aurora Desk Lamp')).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText('Search products'), { target: { value: 'zzz' } });

    const emptyTitle = await screen.findByText('No products match your filters');
    fireEvent.click(
      within(emptyTitle.parentElement as HTMLElement).getByRole('button', {
        name: 'Clear filters',
      }),
    );

    const reloaded = await findProductsTable();
    expect(within(reloaded).getByText('Aurora Desk Lamp')).toBeInTheDocument();
    expect(screen.getByLabelText('Search products')).toHaveValue('');
  });

  it('products_error_state_retries_and_recovers', async () => {
    let calls = 0;
    mockApi([
      sessionRoute(),
      categoryListRoute([]),
      productsRoute(() => {
        calls += 1;
        return calls === 1
          ? jsonResponse(500, {
              error: { code: 'INTERNAL_ERROR', message: 'The server failed to handle the request.' },
            })
          : jsonResponse(200, { data: [product()], pagination: pagination(1, 20, 1, 1) });
      }),
    ]);

    render(<App />);

    expect(await screen.findByText('Products could not be loaded')).toBeInTheDocument();
    expect(screen.getByText('The server failed to handle the request.')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));

    const table = await findProductsTable();
    expect(within(table).getByText('Aurora Desk Lamp')).toBeInTheDocument();
  });

  it('archive_product_confirms_and_refreshes_the_row', async () => {
    let archived = false;
    mockApi([
      sessionRoute(),
      categoryListRoute([]),
      productsRoute(() =>
        jsonResponse(200, {
          data: [archived ? product({ status: 'archived' }) : product()],
          pagination: pagination(1, 20, 1, 1),
        }),
      ),
      {
        method: 'POST',
        path: '/admin/products/5/archive',
        handler: () => {
          archived = true;
          return jsonResponse(200, { data: product({ status: 'archived' }) });
        },
      },
    ]);

    render(<App />);

    const table = await findProductsTable();
    fireEvent.click(within(table).getByRole('button', { name: 'Archive Aurora Desk Lamp' }));

    const dialog = await screen.findByRole('dialog', { name: 'Archive Aurora Desk Lamp?' });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Archive product' }));

    expect(await screen.findByRole('status')).toHaveTextContent('Aurora Desk Lamp was archived.');

    await waitFor(() => {
      const table = screen.getByRole('table', { name: 'Products' });
      expect(within(table).getAllByText('Archived').length).toBeGreaterThan(0);
      expect(
        within(table).queryByRole('button', { name: 'Archive Aurora Desk Lamp' }),
      ).not.toBeInTheDocument();
    });
  });

  it('archive_conflict_surfaces_the_server_message', async () => {
    mockApi([
      sessionRoute(),
      categoryListRoute([]),
      productsRoute(() =>
        jsonResponse(200, { data: [product()], pagination: pagination(1, 20, 1, 1) }),
      ),
      {
        method: 'POST',
        path: '/admin/products/5/archive',
        handler: () =>
          jsonResponse(409, {
            error: { code: 'INVALID_PRODUCT_STATE', message: 'The product is already archived.' },
          }),
      },
    ]);

    render(<App />);

    const table = await findProductsTable();
    fireEvent.click(within(table).getByRole('button', { name: 'Archive Aurora Desk Lamp' }));

    const dialog = await screen.findByRole('dialog', { name: 'Archive Aurora Desk Lamp?' });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Archive product' }));

    expect(await screen.findByText('The product is already archived.')).toBeInTheDocument();
  });
});
